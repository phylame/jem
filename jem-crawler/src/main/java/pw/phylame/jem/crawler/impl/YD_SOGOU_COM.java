/*
 * Copyright 2017 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pw.phylame.jem.crawler.impl;

import lombok.val;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.crawler.AbstractProvider;
import pw.phylame.jem.crawler.CrawlerContext;
import pw.phylame.jem.crawler.Identifiable;
import pw.phylame.jem.crawler.Searchable;
import pw.phylame.jem.crawler.util.HtmlText;
import pw.phylame.jem.crawler.util.SoupUtils;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.ycl.io.HttpUtils;
import pw.phylame.ycl.io.IOUtils;
import pw.phylame.ycl.util.CollectionUtils;
import pw.phylame.ycl.util.DateUtils;
import pw.phylame.ycl.util.Function;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static pw.phylame.ycl.util.StringUtils.*;

public class YD_SOGOU_COM extends AbstractProvider implements Searchable, Identifiable {
    private static final String ENCODING = "UTF-8";
    private static final String HOST = "https://yd.sogou.com";
    private static final String GP = "gf=e-d-pdetail-i&uID=eb8dF3HZM5DeTGBL&sgid=0";

    private String bookKey;

    @Override
    public void init(CrawlerContext context) {
        super.init(context);
        bookKey = valueOfName(context.getAttrUrl().substring(35), "bkey", "&");
    }

    @Override
    public void fetchAttributes() throws IOException {
        ensureInitialized();
        val doc = getSoup(context.getAttrUrl());
        if (doc == null) {
            return;
        }
        Elements soup = doc.select("div.detail-wrap");
        val img = largeImage(soup.select("img").attr("data-echo"));
        Attributes.setCover(book, Flobs.forURL(new URL(img), "image/jpg"));
        soup = soup.select("div.detail-main");
        Attributes.setTitle(book, soup.select("h3.detail-title").text().trim());
        int i = 0;
        for (val node : soup.first().textNodes()) {
            val text = node.text().trim();
            if (isNotEmpty(text)) {
                if (i == 0) {
                    Attributes.setAuthor(book, text);
                } else if (i == 1) {
                    Attributes.setGenre(book, text);
                } else if (i == 2) {
                    Attributes.setState(book, text);
                } else {
                    break;
                }
                ++i;
            }
        }
        Attributes.setDate(book, DateUtils.parse(soup.select("div").get(1).text(), "yyyy-m-D", new Date()));
        soup = doc.select("#l_descr");
        if (soup.isEmpty()) {
            soup = doc.select("p.detail-summary");
        }
        Attributes.setIntro(book, soup.text().trim());
        context.setSoup(doc);
    }

    @Override
    public void fetchContents() throws IOException {
        ensureInitialized();
        for (int i = 2, pages = fetchPage(1); i <= pages; ++i) {
            fetchPage(i);
        }
    }

    @Override
    public String fetchText(String url) {
        val doc = getSoup(url);
        if (doc == null) {
            return "";
        }
        val i = CollectionUtils.map(doc.select("div#text").first().children(), new Function<Element, String>() {
            @Override
            public String apply(Element elem) {
                return elem.text().trim();
            }
        });
        return join(config.lineSeparator, i);
    }

    private int fetchPage(int page) throws IOException {
        val conn = HttpUtils.Request.builder()
                .url(String.format("%s/h5/cpt/ajax/detail?%s&p=%d&bkey=%s&asc=asc", HOST, GP, page, bookKey))
                .method("post")
                .connectTimeout(config.timeout)
                .build()
                .connect();
        val json = new JSONObject(new JSONTokener(IOUtils.readerFor(conn.getInputStream(), ENCODING)));
        val list = json.getJSONObject("list");
        for (val item : list.getJSONArray("items")) {
            val obj = (JSONObject) item;
            val chapter = new Chapter(obj.getString("name"));
            Attributes.setWords(chapter, obj.getInt("size"));
            Attributes.setDate(chapter, new Date(obj.getLong("updateTime")));
            val url = String.format("%s/h5/cpt/chapter?bkey=%s&ckey=%s&%s", HOST, bookKey, obj.getString("ckey"), GP);
            chapter.setText(new HtmlText(url, this, chapter));
            book.append(chapter);
        }
        if (chapterCount == -1) {
            chapterCount = list.getInt("total");
        }
        return list.getInt("totalPages");
    }

    private String largeImage(String url) {
        return url.replaceFirst("w/[\\d]+", "w/2000").replaceFirst("h/[\\d]+", "w/2000");
    }

    @Override
    public String attrUrlOf(String id) {
        return String.format("%s/h5/cpt/detail?bkey=%s&%s", HOST, id, GP);
    }

    @Override
    public List<Map<String, Object>> search(String keywords) throws IOException {
        val doc = Jsoup.connect(String.format("%s/h5/search?query=%s&%s", HOST, keywords, GP))
                .timeout(3000)
                .get();
        val results = new ArrayList<Map<String, Object>>();
        val soup = doc.select("div.result-wrap-match");
        if (soup.isEmpty()) {
            return results;
        }
        val latest = doc.select("div.result-latest");
        results.add(parseResult(soup.first(), latest.get(0)));
        int i = 1;
        for (val element : doc.select("div.result-wrap")) {
            results.add(parseResult(element, latest.get(i++)));
        }
        return results;
    }

    private Map<String, Object> parseResult(Element detail, Element latest) {
        val map = new HashMap<String, Object>();
        map.put("url", String.format("%s/h5/cpt/detail?bkey=%s&%s", HOST, detail.attr("bkey"), GP));
        map.put(Attributes.COVER, largeImage(Selector.select("img", detail).attr("data-echo")));
        map.put(Attributes.TITLE, Selector.select("h3", detail).text().trim());
        val elem = Selector.select("div.result-author", detail).first();
        map.put(Attributes.AUTHOR, SoupUtils.textOf(elem.childNode(0)));
        map.put(Attributes.GENRE, SoupUtils.textOf(elem.childNode(1)));
        map.put(Attributes.STATE, SoupUtils.textOf(elem.childNode(2)));
        map.put(Attributes.INTRO, Selector.select("div.result-summary", detail).text().trim());
        map.put(LAST_UPDATE_KEY, DateUtils.parse(Selector.select("time", latest).text().trim(), "yyyy-m-D", null));
        map.put(LAST_CHAPTER_KEY, Selector.select("em", latest).text().trim());
        return map;
    }
}
