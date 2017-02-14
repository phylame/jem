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

import jem.core.Attributes;
import jem.core.Chapter;
import jem.crawler.*;
import jem.util.flob.Flobs;
import lombok.val;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import pw.phylame.commons.io.HttpUtils;
import pw.phylame.commons.io.IOUtils;
import pw.phylame.commons.io.PathUtils;
import pw.phylame.commons.util.DateUtils;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static pw.phylame.commons.util.StringUtils.*;

public class H5_17K_COM extends AbstractCrawler implements Searchable, Identifiable {
    private static final String ENCODING = "UTF-8";
    private static final String HOST = "http://h5.17k.com";
    private static final int PAGE_SIZE = 180;

    private Chapter section;
    private String bookId;

    @Override
    public void init(Context context) {
        super.init(context);
        bookId = PathUtils.baseName(context.getAttrUrl());
    }

    @Override
    public void fetchAttributes() throws IOException {
        ensureInitialized();
        val doc = getSoup(context.getAttrUrl());
        Elements section = doc.select("section.bookhome_top");
        Attributes.setCover(book, Flobs.forURL(new URL(section.select("img").attr("src")), null));
        Attributes.setTitle(book, section.select("p.title").text());
        Attributes.setAuthor(book, section.select("a.red").text());
        Attributes.setGenre(book, section.select("span").text());
        Attributes.setState(book, section.select("i").get(1).text());
        section = doc.select("section.description");
        Attributes.setIntro(book, joinString(section.select("p").first().textNodes(), config.lineSeparator));
    }

    @Override
    public void fetchContents() throws IOException {
        fetchTocPaged();
    }

    @Override
    public String fetchText(String url) {
        ensureInitialized();
        final Document doc;
        try {
            doc = getSoup(url);
        } catch (IOException e) {
            context.setError(e);
            return EMPTY_TEXT;
        }
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
        return join(config.lineSeparator, lines);
    }

    @Override
    protected int fetchPage(int page) throws IOException {
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
                Attributes.setWords(chapter, obj.getInt("wordCount"));
                Attributes.setDate(chapter, new Date(obj.getLong("updateDate")));
                val url = String.format("%s/chapter/%s/%d.html", HOST, bookId, obj.getLong("id"));
                chapter.setText(new CrawlerText(url, this, chapter));
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
    public String attrUrlOf(String id) {
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
        map.put(Attributes.TITLE, obj.getString("bookName"));
        map.put(Attributes.AUTHOR, obj.getString("authorPenname"));
        map.put(Attributes.STATE, obj.getString("bookStatus"));
        map.put(Attributes.GENRE, obj.getString("categoryName"));
        map.put(Attributes.INTRO, obj.getString("introduction"));
        map.put(Attributes.COVER, "http://img.17k.com/images/bookcover" + obj.getString("coverImageUrl"));
        map.put(LAST_UPDATE_KEY, DateUtils.parse(obj.getString("lastUpdateChapterDate"), "yyyy-m-D H:M", null));
        map.put(LAST_CHAPTER_KEY, obj.getString("lastupdateChapterName"));
        return map;
    }
}
