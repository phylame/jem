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

import lombok.val;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import jem.Chapter;
import jem.crawler.AbstractCrawler;
import jem.crawler.Identifiable;
import jem.crawler.CrawlerText;
import jem.util.flob.Flobs;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import static jem.Attributes.*;
import static pw.phylame.commons.util.StringUtils.*;

public class WWW_ZHULANG_COM extends AbstractCrawler implements Identifiable {
    private static final String HOST = "http://www.zhulang.com";

    @Override
    public String attrUrlOf(String id) {
        return String.format("%s/%s/", HOST, id);
    }

    @Override
    public void fetchAttributes() throws IOException {
        ensureInitialized();
        val doc = getSoup(context.getAttrUrl());
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
        chapterCount = 0;
        val doc = getSoup(context.getAttrUrl().replace("www", "book"));
        for (val div : doc.select("div.bdrbox")) {
            val section = new Chapter(div.select("h2").text().trim());
            val summary = div.select("div.catalog-summary");
            if (!summary.isEmpty()) {
                setIntro(section, trimmed(summary.text()));
            }
            for (val a : div.select("li>a")) {
                val chapter = new Chapter(a.text().trim());
                chapter.setText(new CrawlerText(this, chapter, a.attr("href")));
                section.append(chapter);
                ++chapterCount;
            }
            book.append(section);
        }
    }

    @Override
    protected String fetchText(String url) {
        ensureInitialized();
        final Document doc;
        try {
            doc = getSoup(url);
        } catch (IOException e) {
            context.setError(e);
            return EMPTY_TEXT;
        }
        val lines = new LinkedList<String>();
        for (val elem : doc.select("div#read-content").first().children()) {
            if (!elem.tagName().equals("p")) {
                continue;
            }
            if (elem.childNodeSize() == 1) {
                lines.add(trimmed(elem.text()));
            }
        }
        return join(config.lineSeparator, lines);
    }
}
