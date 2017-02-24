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

package jem.crawler.impl;

import static jem.Attributes.AUTHOR;
import static jem.Attributes.COVER;
import static jem.Attributes.GENRE;
import static jem.Attributes.INTRO;
import static jem.Attributes.STATE;
import static jem.Attributes.TITLE;
import static jem.Attributes.setAuthor;
import static jem.Attributes.setCover;
import static jem.Attributes.setDate;
import static jem.Attributes.setGenre;
import static jem.Attributes.setIntro;
import static jem.Attributes.setState;
import static jem.Attributes.setTitle;
import static jem.Attributes.setWords;
import static pw.phylame.commons.util.StringUtils.join;
import static pw.phylame.commons.util.StringUtils.trimmed;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import jem.Chapter;
import jem.crawler.AbstractCrawler;
import jem.crawler.CrawlerContext;
import jem.crawler.CrawlerText;
import jem.crawler.Identifiable;
import jem.crawler.Searchable;
import jem.util.flob.Flobs;
import lombok.val;
import pw.phylame.commons.io.HttpUtils;
import pw.phylame.commons.io.IOUtils;
import pw.phylame.commons.io.PathUtils;
import pw.phylame.commons.util.DateUtils;

public class H5_17K_COM extends AbstractCrawler implements Searchable, Identifiable {
    private static final String ENCODING = "UTF-8";
    private static final String HOST = "http://h5.17k.com";
    private static final int PAGE_SIZE = 180;

    private Chapter section;
    private String bookId;

    @Override
    public void init(CrawlerContext context) {
        super.init(context);
        bookId = PathUtils.baseName(context.getUrl());
    }

    @Override
    public void fetchAttributes() throws IOException {
        ensureInitialized();
        val book = context.getBook();
        val doc = getSoup(context.getUrl());
        Elements section = doc.select("section.bookhome_top");
        setCover(book, Flobs.forURL(new URL(section.select("img").attr("src")), null));
        setTitle(book, section.select("p.title").text());
        setAuthor(book, section.select("a.red").text());
        setGenre(book, section.select("span").text());
        setState(book, section.select("i").get(1).text());
        section = doc.select("section.description");
        setIntro(book, joinNodes(section.select("p").first().textNodes(), context.getConfig().lineSeparator));
    }

    @Override
    public void fetchContents() throws IOException {
        fetchToc();
    }

    @Override
    public String fetchText(String uri) throws IOException {
        ensureInitialized();
        val doc = getSoup(uri);
        val div = doc.select("div#TextContent");
        val lines = new LinkedList<String>();
        for (val node : div.first().childNodes()) {
            if (!(node instanceof TextNode)) {
                continue;
            }
            val text = (TextNode) node;
            if (!text.isBlank()) {
                lines.add(trimmed(text.text()));
            }
        }
        return join(context.getConfig().lineSeparator, lines);
    }

    @Override
    protected int fetchPage(int page) throws IOException {
        val book = context.getBook();
        val json = getJson(
                String.format("%s/h5/book/ajaxBookList.k?bookId=%s&page=%d&size=%d", HOST, bookId, page, PAGE_SIZE),
                ENCODING);
        Chapter chapter;
        for (val item : json.getJSONArray("datas")) {
            val obj = (JSONObject) item;
            val title = obj.optString("volumeName");
            if (!title.isEmpty()) { // section
                section = section != null ? section.getParent() : book;
                section.append(chapter = new Chapter(title));
                section = chapter;
            } else {
                section.append(chapter = new Chapter(obj.getString("name")));
                setWords(chapter, obj.getInt("wordCount"));
                setDate(chapter, new Date(obj.getLong("updateDate")));
                val url = String.format("%s/chapter/%s/%d.html", HOST, bookId, obj.getLong("id"));
                chapter.setText(new CrawlerText(this, chapter, url));
            }
        }
        if (chapterCount == -1) {
            chapterCount = json.getInt("totalChapter");
            return json.getInt("totalPage");
        } else {
            return 0;
        }
    }

    @Override
    public String urlById(String id) {
        return String.format("%s/book/%s.html", HOST, id);
    }

    @Override
    public List<Map<String, Object>> search(String keywords) throws IOException {
        val conn = HttpUtils.Request.builder()
                .url(String.format("http://search.17k.com/h5/sl?page=1&pageSize=10&searchType=0&q=%s&sortType=5",
                        keywords))
                .connectTimeout(3000)
                .build()
                .connect();
        val results = new ArrayList<Map<String, Object>>(10);
        val json = new JSONObject(new JSONTokener(IOUtils.readerFor(conn.getInputStream(), ENCODING)));
        for (val item : json.getJSONArray("viewList")) {
            results.add(parseItem((JSONObject) item));
        }
        return results;
    }

    private Map<String, Object> parseItem(JSONObject obj) {
        val map = new HashMap<String, Object>();
        map.put(TITLE, obj.getString("bookName"));
        map.put(AUTHOR, obj.getString("authorPenname"));
        map.put(STATE, obj.getString("bookStatus"));
        map.put(GENRE, obj.getString("categoryName"));
        map.put(INTRO, obj.getString("introduction"));
        map.put(COVER, "http://img.17k.com/images/bookcover" + obj.getString("coverImageUrl"));
        map.put(LAST_UPDATE_KEY, DateUtils.parse(obj.getString("lastUpdateChapterDate"), "yyyy-m-D H:M", null));
        map.put(LAST_CHAPTER_KEY, obj.getString("lastupdateChapterName"));
        return map;
    }
}
