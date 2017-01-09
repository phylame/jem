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

import static pw.phylame.ycl.util.StringUtils.EMPTY_TEXT;
import static pw.phylame.ycl.util.StringUtils.firstPartOf;
import static pw.phylame.ycl.util.StringUtils.join;
import static pw.phylame.ycl.util.StringUtils.secondPartOf;
import static pw.phylame.ycl.util.StringUtils.trimmed;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.crawler.AbstractProvider;
import pw.phylame.jem.crawler.Identifiable;
import pw.phylame.jem.crawler.util.HtmlText;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.ycl.util.DateUtils;

public class WWW_MANGG_COM extends AbstractProvider implements Identifiable {
    public static final String HOST = "http://www.mangg.com";

    @Override
    public void fetchAttributes() throws IOException {
        ensureInitialized();
        val doc = getSoup(context.getAttrUrl());
        Elements soup = doc.select("div#info");
        Attributes.setTitle(book, soup.select("h1").text().trim());
        val div = soup.first();
        Attributes.setAuthor(book, secondPartOf(div.child(1).text().trim(), "："));
        String text = div.child(2).text().trim();
        Attributes.setState(book, firstPartOf(secondPartOf(text, "："), ","));
        text = secondPartOf(div.child(3).text().trim(), "：");
        Attributes.setDate(book, DateUtils.parse(text, "yyyy-m-D H:M:S", new Date()));
        val lines = new LinkedList<String>();
        for (val p : doc.select("div#intro").select("p")) {
            lines.add(trimmed(p.text()));
        }
        Attributes.setIntro(book, join(config.lineSeparator, lines));
        val cover = Flobs.forURL(new URL(doc.select("div#fmimg").select("img").attr("src")), null);
        Attributes.setCover(book, cover);
        context.setSoup(doc);
    }

    @Override
    public void fetchContents() throws IOException {
        ensureInitialized();
        val doc = context.getSoup();
        for (val dd : doc.select("dd")) {
            val a = dd.child(0);
            val chapter = new Chapter(a.text().trim());
            chapter.setText(new HtmlText(HOST + a.attr("href"), this, chapter));
            book.append(chapter);
        }
        chapterCount = book.size();
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
        for (val node : doc.select("div#content").first().childNodes()) {
            if (node instanceof TextNode) {
                val text = trimmed(node.toString().replace("&nbsp;", ""));
                if (text.isEmpty() || text.equals(";")) {
                    continue;
                }
                lines.add(text.replace("卝", ""));
            }
        }
        return join(config.lineSeparator, lines);
    }

    @Override
    public String attrUrlOf(String id) {
        return String.format("%s/id%s/", HOST, id);
    }
}
