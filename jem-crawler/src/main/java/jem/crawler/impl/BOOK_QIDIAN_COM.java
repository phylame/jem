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
import jem.crawler.CrawlerText;
import jem.crawler.Identifiable;
import jem.util.flob.Flobs;
import lombok.val;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;

import static jclp.util.StringUtils.firstPartOf;
import static jclp.util.StringUtils.secondPartOf;
import static jem.Attributes.*;

public class BOOK_QIDIAN_COM extends QIDIAN_COM implements Identifiable {
    private static final String HOST = "http://book.qidian.com";

    @Override
    public void fetchAttributes() throws IOException {
        ensureInitialized();
        val context = getContext();
        val book = context.getBook();
        val config = context.getConfig();
        final Document doc = getSoup(context.getUrl());
        Elements soup = doc.select("div.book-information");
        setCover(book, Flobs.forURL(new URL(largeImage(soup.select("div.book-img>a>img").attr("src"))), "image/jpg"));
        soup = soup.select("div.book-info");
        setTitle(book, soup.select("h1>em").text().trim());
        setAuthor(book, soup.select("h1 a").text().trim());
        soup = soup.select("p.tag");
        setWords(book, firstPartOf(soup.first().nextElementSibling().nextElementSibling().text(), "|"));
        setState(book, soup.select("span").first().text());
        setGenre(book, joinNodes(soup.select("a"), "/"));
        setIntro(book, joinNodes(doc.select("div.book-intro>p:eq(0)").first().childNodes(), config.lineSeparator));
    }

    @Override
    public void fetchContents() throws IOException {
        ensureInitialized();
        val context = getContext();
        val book = context.getBook();
        final Document doc = getSoup(context.getUrl() + "#Catalog");
        for (val volume : doc.select("div.volume")) {
            val section = new Chapter(secondPartOf(volume.select("h3").text().split("·")[0], " "));
            setWords(section, Integer.parseInt((volume.select("h3 cite").text().trim())));
            book.append(section);
            for (val a : volume.select("ul a")) {
                val chapter = new Chapter(a.text().trim());
                val text = new CrawlerText(this, chapter, protocol + a.attr("href"));
                chapter.setText(text);
                onTextAdded(text);
                section.append(chapter);
            }
        }
    }

    @Override
    public String fetchText(String uri) throws IOException {
        ensureInitialized();
        return joinNodes(getSoup(uri).select("div.read-content>p"), getContext().getConfig().lineSeparator);
    }

    @Override
    public String urlById(String id) {
        return String.format("%s/info/%s", HOST, id);
    }
}
