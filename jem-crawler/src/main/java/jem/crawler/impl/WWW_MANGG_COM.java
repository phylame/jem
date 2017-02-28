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
import jem.crawler.AbstractCrawler;
import jem.crawler.CrawlerText;
import jem.crawler.Identifiable;
import jem.util.flob.Flobs;
import lombok.val;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import pw.phylame.commons.log.Log;
import pw.phylame.commons.util.DateUtils;
import pw.phylame.commons.util.StringUtils;
import pw.phylame.commons.util.Validate;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;

import static jem.Attributes.*;
import static pw.phylame.commons.util.StringUtils.*;

public class WWW_MANGG_COM extends AbstractCrawler implements Identifiable {
    private static final String HOST = "http://www.mangg.com";

    @Override
    public void fetchAttributes() throws IOException {
        ensureInitialized();
        val book = context.getBook();
        val config = context.getConfig();
        final Document doc;
        try {
            doc = getSoup(context.getUrl());
        } catch (InterruptedException e) {
            Log.d(TAG, "user interrupted");
            return;
        }
        Elements soup = doc.select("div#info");
        setTitle(book, soup.select("h1").text().trim());
        val div = soup.first();
        setAuthor(book, secondPartOf(div.child(1).text().trim(), "："));
        String text = div.child(2).text().trim();
        setState(book, firstPartOf(secondPartOf(text, "："), ","));
        text = secondPartOf(div.child(3).text().trim(), "：");
        setDate(book, DateUtils.parse(text, "yyyy-m-D H:M:S", new Date()));
        val lines = new LinkedList<String>();
        for (val p : doc.select("div#intro").select("p")) {
            lines.add(joinNodes(p.textNodes(), config.lineSeparator));
        }
        setIntro(book, join(config.lineSeparator, lines));
        val cover = Flobs.forURL(new URL(doc.select("div#fmimg").select("img").attr("src")), null);
        setCover(book, cover);
        context.setSoup(doc);
    }

    @Override
    public void fetchContents() throws IOException {
        ensureInitialized();
        val book = context.getBook();
        val doc = context.getSoup();
        Validate.checkNotNull(doc, "soup should have been initialized");
        for (val dd : doc.select("dd")) {
            val a = dd.child(0);
            val chapter = new Chapter(a.text().trim());
            val text = new CrawlerText(this, chapter, HOST + a.attr("href"));
            book.getTexts().add(text);
            chapter.setText(text);
            book.append(chapter);
        }
    }

    @Override
    protected String fetchText(String uri) throws IOException {
        ensureInitialized();
        final Document doc;
        try {
            doc = getSoup(uri);
        } catch (InterruptedException e) {
            Log.d(TAG, "user interrupted");
            return StringUtils.EMPTY_TEXT;
        }
        val lines = new LinkedList<String>();
        for (val node : doc.select("div#content").first().childNodes()) {
            if (node instanceof TextNode) {
                val text = trimmed(node.toString().replace("&nbsp;", ""));
                if (text.isEmpty() || text.equals(";")) {
                    continue;
                }
                lines.add(text.replace("卝", ""));
            }
        }
        return join(context.getConfig().lineSeparator, lines);
    }

    @Override
    public String urlById(String id) {
        return String.format("%s/id%s/", HOST, id);
    }
}
