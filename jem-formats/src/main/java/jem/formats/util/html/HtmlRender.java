/*
 * Copyright 2014-2017 Peng Wan <phylame@163.com>
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

package jem.formats.util.html;

import jem.epm.util.MakerException;
import jem.epm.util.xml.XmlConfig;
import jem.epm.util.xml.XmlRender;
import jem.util.text.Text;
import lombok.NonNull;
import lombok.val;
import jclp.util.CollectionUtils;
import jclp.util.StringUtils;
import jclp.util.Validate;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;

/**
 * Renders book content with HTML.
 */
public class HtmlRender {
    private static final String DT_ID = "-//W3C//DTD XHTML 1.1//EN";
    private static final String DT_URI = "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd";
    private static final String NAMESPACE = "http://www.w3.org/1999/xhtml";

    public static class Link {
        public final String title;
        public final String href;

        public Link(String title, String href) {
            this.title = title;
            this.href = href;
        }
    }

    private final HtmlConfig htmlConfig;
    private final XmlRender xmlRender;

    public HtmlRender(@NonNull HtmlConfig config) throws MakerException {
        Validate.requireNotNull(config.htmlLanguage, "Not specify htmlLanguage of HtmlConfig");
        Validate.requireNotNull(config.cssHref, "Not specify cssHref of HtmlConfig");
        val xmlConfig = new XmlConfig();
        xmlConfig.encoding = config.encoding;
        xmlConfig.standalone = true;
        xmlConfig.lineSeparator = "\r\n";
        xmlConfig.indentString = config.indentString;
        xmlRender = new XmlRender(xmlConfig, false);
        htmlConfig = config;
    }

    public void setOutput(OutputStream outputStream) throws IOException {
        xmlRender.setOutput(outputStream);
    }

    public void setOutput(Writer writer) throws IOException {
        xmlRender.setOutput(writer);
    }

    /*
     * $common-image: href, $style
     */
    public void renderCover(String title, String href, String alt) throws IOException {
        beginHtml(title);
        writeImage(href, alt, htmlConfig.style.bookCover);
        endHtml();
    }

    /*
     * <div class="$style">
     *   <h1>book-title</h1>
     * </div>
     * $common-title: intro-title, $style
     * $common-text: intro, $style
     */
    public void renderIntro(String title, String bookTitle, String introTitle, Text intro) throws IOException {
        renderCoverIntro(title, null, null, bookTitle, introTitle, intro);
    }

    /*
     * $common-image: cover, $style (when cover is not null)
     * <div class="$style">
     *   <h1>book-title</h1>
     * </div>
     * $common-title: intro-title, $style
     * $common-text: intro, $style
     */
    public void renderCoverIntro(String title, String cover, String alt, String bookTitle, String introTitle,
                                 Text intro) throws IOException {
        beginHtml(title);
        if (cover != null) {
            writeImage(cover, alt, htmlConfig.style.bookCover);
        }

        if (intro != null) {
            xmlRender.beginTag("div").attribute("class", htmlConfig.style.bookTitle);
            xmlRender.beginTag("h1")
                    .text(bookTitle)
                    .endTag();
            xmlRender.endTag();

            writeTitle(introTitle, htmlConfig.style.introTitle);
            writeText(intro, htmlConfig.style.introText);
        }

        endHtml();
    }

    /*
     * $common-title: title, $style
     * $common-contents: titles, links, $style
     */
    public void renderToc(String title, List<Link> links) throws IOException {
        beginHtml(title);
        writeTitle(title, htmlConfig.style.tocTitle);
        writeContents(links, htmlConfig.style.tocItems);
        endHtml();
    }

    /*
     * $common-image: href, $style
     */
    public void renderSectionCover(String title, String href, String alt) throws IOException {
        renderCover0(title, href, alt, htmlConfig.style.sectionCover);
    }

    /*
     * $common-part: title, $style, intro, $style
     * $common-contents: titles, links, $style
     */
    public void renderSection(String title, Text intro, List<Link> links) throws IOException {
        renderSection(title, null, null, intro, links);
    }

    /*
     * $common-cover: cover, $style (when cover is not null)
     * $common-part: title, $style, intro, $style
     * $common-contents: titles, links, $style
     */
    public void renderSection(String title, String cover, String alt, Text intro,
                              List<Link> links) throws IOException {
        beginHtml(title);
        if (cover != null) {
            writeImage(cover, alt, htmlConfig.style.sectionCover);
        }
        writePart(title, htmlConfig.style.sectionTitle, intro, htmlConfig.style.sectionIntro);
        writeContents(links, htmlConfig.style.sectionItems);
        endHtml();
    }

    /*
     * $common-image: href, $style
     */
    public void renderChapterCover(String title, String href, String alt) throws IOException {
        renderCover0(title, href, alt, htmlConfig.style.chapterCover);
    }

    /*
     * $common-part: title, $style, intro, $style
     * $common-text: content, $style
     */
    public void renderChapter(String title, Text intro, Text content) throws IOException {
        renderChapter(title, null, null, intro, content);
    }

    /*
     * $common-cover: cover, $style (when cover is not null)
     * $common-part: title, $style, intro, $style
     * $common-text: content, $style
     */
    public void renderChapter(String title, String cover, String alt, Text intro, Text content) throws IOException {
        beginHtml(title);
        if (cover != null) {
            writeImage(cover, alt, htmlConfig.style.chapterCover);
        }
        writePart(title, htmlConfig.style.chapterTitle, intro, htmlConfig.style.chapterIntro);
        writeText(content, htmlConfig.style.chapterText);
        endHtml();
    }

    /*
     * $common-image: href, style
     */
    private void renderCover0(String title, String href, String alt, String style) throws IOException {
        beginHtml(title);
        writeImage(href, alt, style);
        endHtml();
    }

    /*
     * <div class="style">
     *   <p><a href="link">title</a></p>
     *   ...
     *   <p><a href="link">title</a></p>
     * </div>
     */
    private void writeContents(List<Link> links, String style) throws IOException {
        xmlRender.beginTag("div").attribute("class", style);
        for (val link : links) {
            xmlRender.beginTag("p");
            xmlRender.beginTag("a")
                    .attribute("href", link.href)
                    .text(link.title)
                    .endTag();
            xmlRender.endTag();
        }
        xmlRender.endTag();
    }

    /*
     * <div class="title-style">
     *   <h3>title</h3>
     * </div>
     * $common-text: intro, intro-style (when intro is not null)
     */
    private void writePart(String title, String titleStyle, Text intro, String introStyle) throws IOException {
        writeTitle(title, titleStyle);
        if (intro != null) {
            writeText(intro, introStyle);
        }
    }

    /*
     * <div class="style">
     *   <p>trimmed-line</p>
     *   ...
     *   <p>trimmed-line</p>
     * </div>
     */
    private void writeText(Text text, String style) throws IOException {
        val lines = text.getLines(htmlConfig.skipEmpty);
        if (CollectionUtils.isEmpty(lines)) {
            return;
        }
        xmlRender.beginTag("div").attribute("class", style);
        for (val line : lines) {
            xmlRender.beginTag("p").text(StringUtils.trimmed(line)).endTag();
        }
        xmlRender.endTag();
    }

    /*
     * <div class="style">
     *   <img src="href"/>
     * </div>
     */
    private void writeImage(String href, String alt, String style) throws IOException {
        xmlRender.beginTag("div").attribute("class", style);
        xmlRender.beginTag("img").attribute("src", href);
        xmlRender.attribute("alt", alt);
        xmlRender.endTag();
        xmlRender.endTag();
    }

    /*
     * <div class="style">
     *   <h3>title</h3>
     * </div>
     */
    private void writeTitle(String title, String style) throws IOException {
        xmlRender.beginTag("div").attribute("class", style);
        xmlRender.beginTag("h3").text(title).endTag();
        xmlRender.endTag();
    }

    private void beginHtml(String title) throws IOException {
        xmlRender.beginXml();
        xmlRender.docdecl("html", DT_ID, DT_URI);
        xmlRender.beginTag("html");
        xmlRender.attribute("xmlns", NAMESPACE);
        xmlRender.attribute("xml:lang", htmlConfig.htmlLanguage);

        // head
        xmlRender.beginTag("head");

        xmlRender.beginTag("meta")
                .attribute("http-equiv", "Content-Type")
                .attribute("content", "text/html; charset=" + htmlConfig.encoding)
                .endTag();

        // custom meta info
        if (htmlConfig.metaInfo != null && !htmlConfig.metaInfo.isEmpty()) {
            for (val entry : htmlConfig.metaInfo.entrySet()) {
                xmlRender.beginTag("meta")
                        .attribute("name", entry.getKey())
                        .attribute("content", entry.getValue())
                        .endTag();
            }
        }

        // CSS link
        xmlRender.beginTag("link")
                .attribute("type", "text/css")
                .attribute("rel", "stylesheet")
                .attribute("href", htmlConfig.cssHref)
                .endTag();

        // html title
        xmlRender.beginTag("title").text(title).endTag();

        xmlRender.endTag(); // head

        xmlRender.beginTag("body");
    }

    private void endHtml() throws IOException {
        xmlRender.endTag(); // body
        xmlRender.endTag(); // html
        xmlRender.endXml();
    }
}
