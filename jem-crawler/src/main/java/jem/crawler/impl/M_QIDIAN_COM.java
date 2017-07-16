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

import jclp.io.IOUtils;
import jem.Chapter;
import jem.crawler.CrawlerText;
import jem.crawler.Identifiable;
import jem.util.flob.Flobs;
import lombok.val;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import static jclp.util.StringUtils.*;
import static jem.Attributes.*;

public class M_QIDIAN_COM extends QIDIAN_COM implements Identifiable {
    private static final String HOST = "http://m.qidian.com";

    @Override
    public void fetchAttributes() throws IOException {
        ensureInitialized();
        val context = getContext();
        val book = context.getBook();
        final Document doc = getSoup(context.getUrl());
        Elements soup = doc.select("div.book-layout");
        setCover(book, Flobs.forURL(new URL(largeImage(soup.select("img").attr("src"))), "image/jpg"));
        setTitle(book, soup.select("h2")
                .text()
                .trim());
        setAuthor(book, soup.select("div.book-rand-a>a")
                .first()
                .textNodes()
                .get(0)
                .text()
                .trim());
        soup = soup.select("p");
        setGenre(book, soup.first()
                .text()
                .trim());
        val pair = partition(soup.get(1)
                .text()
                .trim(), "|");
        setWords(book, pair.getFirst());
        setState(book, pair.getSecond());
        val lines = new LinkedList<String>();
        for (val str : trimmed(doc.select("section#bookSummary").select("textarea").text()).split("<br>")) {
            lines.add(trimmed(str));
        }
        setIntro(book, join(context.getConfig().lineSeparator, lines));
    }

    @Override
    public void fetchContents() throws IOException {
        ensureInitialized();
        val context = getContext();
        val book = context.getBook();
        val str = IOUtils.toString(getContent(context.getUrl() + "/catalog", "get"), "utf-8");
        val start = str.indexOf("[{", str.indexOf("g_data.volumes"));
        val end = str.indexOf("}];", start);
        Chapter section;
        val baseURL = context.getUrl();
        for (val s : new JSONArray(str.substring(start, end + 2))) {
            JSONObject json = (JSONObject) s;
            book.append(section = new Chapter(json.getString("vN")));
            for (val c : json.getJSONArray("cs")) {
                json = (JSONObject) c;
                val chapter = new Chapter(json.getString("cN"));
                setWords(chapter, json.getInt("cnt"));
                val text = new CrawlerText(this, chapter, baseURL + '/' + json.getInt("id"));
                chapter.setText(text);
                onTextAdded(text);
                section.append(chapter);
            }
        }
    }

    @Override
    public String fetchText(String uri) throws IOException {
        ensureInitialized();
        val nodes = getSoup(uri)
                .select("section.read-section")
                .select("p");
        return joinNodes(nodes, getContext().getConfig().lineSeparator);
    }

    @Override
    public String urlById(String id) {
        return String.format("http://m.qidian.com/book/%s", id);
    }

}
