/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
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

package pw.phylame.jem.formats.util.html;

import pw.phylame.jem.epm.util.MakerException;
import pw.phylame.jem.epm.util.xml.XmlConfig;
import pw.phylame.jem.epm.util.xml.XmlRender;
import pw.phylame.jem.util.text.Text;
import pw.phylame.ycl.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.Map;

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

    private final HtmlConfig config;
    private final XmlRender xmlRender;

    public HtmlRender(HtmlConfig config) throws MakerException {
        if (config.htmlLanguage == null) {
            throw new IllegalArgumentException("Not specify htmlLanguage of HtmlConfig");
        }
        if (config.cssHref == null) {
            throw new IllegalArgumentException("Not specify cssHref of HtmlConfig");
        }
        this.config = config;
        XmlConfig xmlConfig = new XmlConfig();
        xmlConfig.encoding = config.encoding;
        xmlConfig.standalone = true;
        xmlConfig.lineSeparator = "\r\n";
        xmlConfig.indentString = config.indentString;
        xmlRender = new XmlRender(xmlConfig, false);
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
        writeImage(href, alt, config.style.bookCover);
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
            writeImage(cover, alt, config.style.bookCover);
        }

        if (intro != null) {
            xmlRender.startTag("div").attribute("class", config.style.bookTitle);
            xmlRender.startTag("h1").text(bookTitle).endTag();
            xmlRender.endTag();

            writeTitle(introTitle, config.style.introTitle);
            writeText(intro, config.style.introText);
        }

        endHtml();
    }

    /*
     * $common-title: title, $style
     * $common-contents: titles, links, $style
     */
    public void renderToc(String title, List<Link> links) throws IOException {
        beginHtml(title);
        writeTitle(title, config.style.tocTitle);
        writeContents(links, config.style.tocItems);
        endHtml();
    }

    /*
     * $common-image: href, $style
     */
    public void renderSectionCover(String title, String href, String alt) throws IOException {
        renderCover0(title, href, alt, config.style.sectionCover);
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
            writeImage(cover, alt, config.style.sectionCover);
        }
        writePart(title, config.style.sectionTitle, intro, config.style.sectionIntro);
        writeContents(links, config.style.sectionItems);
        endHtml();
    }

    /*
     * $common-image: href, $style
     */
    public void renderChapterCover(String title, String href, String alt) throws IOException {
        renderCover0(title, href, alt, config.style.chapterCover);
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
    public void renderChapter(String title, String cover, String alt, Text intro,
                              Text content) throws IOException {
        beginHtml(title);
        if (cover != null) {
            writeImage(cover, alt, config.style.chapterCover);
        }
        writePart(title, config.style.chapterTitle, intro, config.style.chapterIntro);
        writeText(content, config.style.chapterText);
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
        xmlRender.startTag("div").attribute("class", style);
        for (Link link : links) {
            xmlRender.startTag("p");
            xmlRender.startTag("a").attribute("href", link.href);
            xmlRender.text(link.title).endTag();
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
    private void writePart(String title, String titleStyle, Text intro, String introStyle)
            throws IOException {
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
        List<String> lines = text.getLines(config.skipEmpty);
        if (lines == null || lines.isEmpty()) {
            return;
        }
        xmlRender.startTag("div").attribute("class", style);
        for (String line : lines) {
            xmlRender.startTag("p").text(StringUtils.trimmed(line)).endTag();
        }
        xmlRender.endTag();
    }

    /*
     * <div class="style">
     *   <img src="href"/>
     * </div>
     */
    private void writeImage(String href, String alt, String style) throws IOException {
        xmlRender.startTag("div").attribute("class", style);
        xmlRender.startTag("img").attribute("src", href);
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
        xmlRender.startTag("div").attribute("class", style);
        xmlRender.startTag("h3").text(title).endTag();
        xmlRender.endTag();
    }

    private void beginHtml(String title) throws IOException {
        xmlRender.startXml();
        xmlRender.docdecl("html", DT_ID, DT_URI);
        xmlRender.startTag("html");
        xmlRender.attribute("xmlns", NAMESPACE);
        xmlRender.attribute("xml:lang", config.htmlLanguage);

        // head
        xmlRender.startTag("head");

        xmlRender.startTag("meta");
        xmlRender.attribute("http-equiv", "Content-Type");
        xmlRender.attribute("content", "text/html; charset=" + config.encoding);
        xmlRender.endTag();

        // custom meta info
        if (config.metaInfo != null && !config.metaInfo.isEmpty()) {
            for (Map.Entry<String, String> entry : config.metaInfo.entrySet()) {
                xmlRender.startTag("meta");
                xmlRender.attribute("name", entry.getKey());
                xmlRender.attribute("content", entry.getValue());
                xmlRender.endTag();
            }
        }

        // CSS link
        xmlRender.startTag("link");
        xmlRender.attribute("type", "text/css");
        xmlRender.attribute("rel", "stylesheet");
        xmlRender.attribute("href", config.cssHref);
        xmlRender.endTag();

        // html title
        xmlRender.startTag("title").text(title).endTag();

        xmlRender.endTag(); // head

        xmlRender.startTag("body");
    }

    private void endHtml() throws IOException {
        xmlRender.endTag(); // body
        xmlRender.endTag(); // html
        xmlRender.endXml();
    }
}
