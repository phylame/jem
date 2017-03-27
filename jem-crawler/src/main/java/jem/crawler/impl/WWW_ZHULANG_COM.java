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

import jem.Chapter;
import jem.crawler.CrawlerProvider;
import jem.crawler.CrawlerText;
import jem.crawler.Identifiable;
import jem.util.flob.Flobs;
import lombok.val;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import static jem.Attributes.*;
import static pw.phylame.commons.util.StringUtils.*;

public class WWW_ZHULANG_COM extends CrawlerProvider implements Identifiable {
    private static final String HOST = "http://www.zhulang.com";

    @Override
    public String urlById(String id) {
        return String.format("%s/%s/", HOST, id);
    }

    @Override
    public void fetchAttributes() throws IOException {
        ensureInitialized();
        val context = getContext();
        val book = context.getBook();
        final Document doc = getSoup(context.getUrl());
        setCover(book, Flobs.forURL(new URL(doc.select("div.cover-box-left>img").attr("src")), null));
        Elements soup = doc.select("div.cover-box-right");
        Element elem = soup.select("h2").first();
        setTitle(book, elem.childNode(0).toString().trim());
        setAuthor(book, secondPartOf(((Element) elem.childNode(1)).text().trim(), "："));
        setGenre(book, secondPartOf(soup.select("span").get(1).text().trim(), "："));
        setWords(book, Integer.parseInt(secondPartOf(soup.select("span").get(3).text().trim(), "：")));
        setIntro(book, trimmed(soup.select("div#book-summary>p.summ-all").first().childNode(0).toString()));
    }

    @Override
    public void fetchContents() throws IOException {
        ensureInitialized();
        val context = getContext();
        val book = context.getBook();
        final Document doc = getSoup(context.getUrl().replace("www", "book"));
        for (val div : doc.select("div.bdrbox")) {
            val section = new Chapter(div.select("h2").text().trim());
            val summary = div.select("div.catalog-summary");
            if (!summary.isEmpty()) {
                setIntro(section, trimmed(summary.text()));
            }
            for (val a : div.select("li>a")) {
                val chapter = new Chapter(a.text().trim());
                val text = new CrawlerText(this, chapter, a.attr("href"));
                onTextAdded(text);
                chapter.setText(text);
                section.append(chapter);
            }
            book.append(section);
        }
    }

    @Override
    public String fetchText(String uri) throws IOException {
        ensureInitialized();
        final Document doc = getSoup(uri);
        val lines = new LinkedList<String>();
        for (val elem : doc.select("div#read-content").first().children()) {
            if (!elem.tagName().equals("p")) {
                continue;
            }
            if (elem.childNodeSize() == 1) {
                lines.add(trimmed(elem.text()));
            }
        }
        return join(getContext().getConfig().lineSeparator, lines);
    }
}
