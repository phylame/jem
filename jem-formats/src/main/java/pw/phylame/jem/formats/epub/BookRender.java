/*
 * Copyright 2016 Peng Wan <phylame@163.com>
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

package pw.phylame.jem.formats.epub;

import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.epm.util.MakerException;
import pw.phylame.jem.formats.epub.writer.EpubWriter;
import pw.phylame.jem.formats.util.JFMessages;
import pw.phylame.jem.formats.util.html.HtmlRender;
import pw.phylame.jem.formats.util.html.StyleProvider;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.jem.util.text.Text;
import pw.phylame.jem.util.text.Texts;
import pw.phylame.ycl.io.PathUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes book HTML tree.
 */
public class BookRender {
    // for DuoKan reader full screen image
    public static final String DUOKAN_FULL_SCREEN = "duokan-page-fullscreen";

    private static final String STYLE_DIR = "Styles";
    private static final String IMAGE_DIR = "Images";
    private static final String TEXT_DIR = "Text";

    public static final String MT_XHTML = "application/xhtml+xml";

    public static final String COVER_NAME = "cover";

    // main CSS
    public static final String CSS_FILE = "stylesheet.css";
    public static final String CSS_FILE_ID = "main-css";
    public static final String MT_CSS = "text/css";

    public static final String INTRO_NAME = "intro";

    public static final String TOC_NAME = "toc";

    private final Book book;
    private final EpubWriter epubWriter;
    private final EpubOutConfig epubConfig;
    private HtmlRender htmlRender;
    private final ZipOutputStream zipout;
    private final BookListener bookListener;

    public BookRender(EpubWriter epubWriter, BookListener bookListener, OutTuple tuple) {
        this.book = tuple.book;
        this.epubWriter = epubWriter;
        this.epubConfig = tuple.config;
        this.zipout = tuple.zipout;
        this.bookListener = bookListener;
    }

    private String coverID;
    private final List<Resource> resources = new LinkedList<>();
    private final List<Spine> spines = new LinkedList<>();
    private final List<Guide> guides = new LinkedList<>();

    private void newResource(String id, String href, String mime) {
        resources.add(new Resource(id, href, mime));
    }

    private void newGuideItem(String href, String type, String title) {
        guides.add(new Guide(href, type, title));
    }

    private void newSpineItem(String id, boolean linear, String properties) {
        spines.add(new Spine(id, linear, properties));
    }

    private void newNaviItem(String id, String href, String title, String properties) throws IOException {
        newSpineItem(id, true, properties);
        bookListener.startNavPoint(id, href, title);
    }

    private void endNaviItem() throws IOException {
        bookListener.endNavPoint();
    }

    public String getCoverID() {
        return coverID;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public List<Spine> getSpineItems() {
        return spines;
    }

    public List<Guide> getGuideItems() {
        return guides;
    }

    public void start() throws IOException, MakerException {
        if (!book.isSection()) {    // no sub-chapter
            return;
        }
        writeGlobalCss();
        htmlRender = new HtmlRender(epubConfig.htmlConfig);
        writeBookCover();
        writeToc();
    }

    private void writeBookCover() throws IOException {
        val cover = Attributes.getCover(book);
        if (cover == null) {    // no cover write intro
            writeIntroPage();
        } else {
            val title = JFMessages.tr("epub.page.cover.title");
            coverID = COVER_NAME + "-image";
            val href = writeImage(cover, COVER_NAME, coverID);
            if (epubConfig.smallPage) {   // for phone split to cover and intro page
                writeCoverPage(title, href);
                writeIntroPage();
            } else {    // one page
                writeCoverIntro(title, href);
            }
        }
    }

    private void writeCoverPage(String title, String coverHref) throws IOException {
        val writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderCover(title, coverHref, title);
        val href = writeText(writer.toString(), COVER_NAME, hrefOfText(COVER_NAME));
        newSpineItem(COVER_NAME, true, DUOKAN_FULL_SCREEN);
        newGuideItem(href, "cover", title);
    }

    private void writeIntroPage() throws IOException {
        val intro = Attributes.getIntro(book);
        if (intro == null) {    // no book intro
            return;
        }
        val title = JFMessages.tr("epub.page.intro.title");
        val baseName = INTRO_NAME;
        val writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderIntro(title, Attributes.getTitle(book), title, intro);
        val href = writeText(writer.toString(), baseName, hrefOfText(baseName));
        newNaviItem(baseName, href, title, null);
        endNaviItem();
    }

    private void writeCoverIntro(String title, String coverHref) throws IOException {
        val writer = new StringWriter();
        htmlRender.setOutput(writer);
        Text intro = Attributes.getIntro(book);
        if (intro != null) {
            val bookTitle = Attributes.getTitle(book);
            htmlRender.renderCoverIntro(title, coverHref, bookTitle, bookTitle, JFMessages.tr("epub.page.intro.title"), intro);
        } else {
            htmlRender.renderCover(title, coverHref, title);
        }
        val href = writeText(writer.toString(), COVER_NAME, hrefOfText(COVER_NAME));
        newNaviItem(COVER_NAME, href, title, DUOKAN_FULL_SCREEN);
        endNaviItem();
        newGuideItem(href, "cover", title);
    }

    private void writeToc() throws IOException {
        val title = JFMessages.tr("epub.page.toc.title");
        val href = TEXT_DIR + "/" + hrefOfText(TOC_NAME);
        newNaviItem(TOC_NAME, href, title, null);
        endNaviItem();
        // sections and chapters
        val links = processSection(book, "", hrefOfText(TOC_NAME));
        val writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderToc(title, links);
        writeText(writer.toString(), TOC_NAME, hrefOfText(TOC_NAME));
        newGuideItem(href, "toc", title);
    }

    // return links of sub-chapters
    private List<HtmlRender.Link> processSection(Chapter section, String suffix, String myHref) throws IOException {
        val links = new LinkedList<HtmlRender.Link>();
        String mySuffix;
        int count = 1;
        for (Chapter sub : section) {
            mySuffix = suffix + "-" + Integer.toString(count);
            links.add(!sub.isSection() ? writeChapter(sub, mySuffix) : writeSection(sub, mySuffix, myHref));
            ++count;
        }
        return links;
    }

    private HtmlRender.Link writeSection(Chapter section, String suffix, String parentHref) throws IOException {
        val baseName = "section" + suffix;
        val name = hrefOfText(baseName);
        val href = TEXT_DIR + "/" + name;
        val sectionTitle = Attributes.getTitle(section);

        String coverHref = writePartCover(section, baseName);
        if (coverHref != null && epubConfig.smallPage) {
            writeSectionCover(sectionTitle, coverHref, baseName);
            coverHref = null;
        }
        newNaviItem(baseName, href, sectionTitle, null);

        // sub-chapters
        val myHref = hrefOfText("section" + suffix);
        val links = processSection(section, suffix, myHref);
        if (parentHref != null) {
            links.add(new HtmlRender.Link(JFMessages.tr("epub.page.contents.gotoTop"), parentHref));
        }

        val writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderSection(sectionTitle, coverHref, sectionTitle, Attributes.getIntro(section), links);
        writeText(writer.toString(), baseName, name);
        endNaviItem();
        return new HtmlRender.Link(sectionTitle, name);
    }

    /**
     * Writes specified chapter to OPS text directory.
     *
     * @param chapter the chapter
     * @param suffix  suffix string for file path
     * @return link to the chapter HTML, relative to HTML in textDir
     * @throws IOException if occur IO errors
     */
    private HtmlRender.Link writeChapter(Chapter chapter, String suffix) throws IOException {
        val baseName = "chapter" + suffix;
        val name = hrefOfText(baseName);
        val href = TEXT_DIR + "/" + name;
        val chapterTitle = Attributes.getTitle(chapter);

        val text = chapter.getText();
        if (text != null && text.getType().equals(Text.HTML)) {    // text already HTML
            newNaviItem(baseName, writeText(text, baseName, name), chapterTitle, null);
            endNaviItem();
            return new HtmlRender.Link(chapterTitle, name);
        }

        String coverHref = writePartCover(chapter, baseName);
        if (coverHref != null && epubConfig.smallPage) {
            writeChapterCover(chapterTitle, coverHref, baseName);
            coverHref = null;
        }

        val zipEntry = new ZipEntry(epubWriter.pathInOps(href));
        zipout.putNextEntry(zipEntry);
        htmlRender.setOutput(zipout);
        htmlRender.renderChapter(chapterTitle, coverHref, chapterTitle, Attributes.getIntro(chapter), text != null
                ? text
                : Texts.forEmpty(Text.PLAIN));
        zipout.closeEntry();

        newResource(baseName, href, MT_XHTML);
        newNaviItem(baseName, href, chapterTitle, null);
        endNaviItem();
        return new HtmlRender.Link(chapterTitle, name);
    }

    private void writeChapterCover(String title, String coverHref, String baseName) throws IOException {
        val id = baseName + "-" + COVER_NAME;
        val writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderChapterCover(title, coverHref, title);
        writeText(writer.toString(), id, hrefOfText(id));
        newSpineItem(id, true, DUOKAN_FULL_SCREEN);
        endNaviItem();
    }

    private void writeSectionCover(String title, String coverHref, String baseName) throws IOException {
        val id = baseName + "-" + COVER_NAME;
        val writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderSectionCover(title, coverHref, title);
        writeText(writer.toString(), id, hrefOfText(id));
        newSpineItem(id, true, DUOKAN_FULL_SCREEN);
    }

    private String writePartCover(Chapter chapter, String baseName) throws IOException {
        val cover = Attributes.getCover(chapter);
        if (cover == null) {
            return null;
        }
        val name = baseName + "-" + COVER_NAME;
        return writeImage(cover, name, name + "-image");
    }

    /**
     * Writes specified image to OPS image directory.
     *
     * @param file the image file
     * @param name base name of image (no extension)
     * @return path relative to HTML in textDir
     * @throws IOException if occur IO errors
     */
    private String writeImage(Flob file, String name, String id) throws IOException {
        val path = IMAGE_DIR + "/" + name + "." + PathUtils.extensionName(file.getName());
        writeIntoEpub(file, path, id, file.getMime());
        return "../" + path;    // relative to HTML in textDir
    }

    private String hrefOfText(String baseName) {
        return baseName + ".xhtml";
    }

    /**
     * Writes specified HTML text to OPS text directory.
     *
     * @param text the HTML string
     * @param id   id of the HTML file
     * @param name name of HTML file
     * @return path in OPS
     * @throws IOException if occur IO errors
     */
    private String writeText(String text, String id, String name) throws IOException {
        val path = TEXT_DIR + "/" + name;
        epubWriter.writeToOps(text, path, epubConfig.htmlConfig.encoding);
        newResource(id, path, MT_XHTML);
        return path;
    }

    /**
     * Writes specified HTML text to OPS text directory.
     *
     * @param text the <tt>Text</tt> containing HTML
     * @param id   id of the HTML file
     * @param name name of HTML
     * @return path in OPS
     * @throws IOException if occur IO errors
     */
    private String writeText(Text text, String id, String name) throws IOException {
        val path = TEXT_DIR + "/" + name;
        epubWriter.writeToOps(text, path, epubConfig.htmlConfig.encoding);
        newResource(id, path, MT_XHTML);
        return path;
    }

    /**
     * Writes CSS file of HtmlConfig to OPS style directory.
     * <p>After writing, the cssHref of HtmlConfig will be assigned.
     *
     * @throws IOException if occur IO errors
     */
    private void writeGlobalCss() throws IOException {
        if (epubConfig.htmlConfig.style == null) {
            epubConfig.htmlConfig.style = StyleProvider.getDefaults();
        }
        val name = STYLE_DIR + "/" + CSS_FILE;
        writeIntoEpub(epubConfig.htmlConfig.style.cssFile, name, CSS_FILE_ID, MT_CSS);
        epubConfig.htmlConfig.cssHref = "../" + name;   // relative to HTML in textDir
    }

    /**
     * Writes file in OPS to ePub archive.
     *
     * @param file      the  file
     * @param path      path in ops
     * @param id        id of the file
     * @param mediaType media type of the file,
     *                  if <tt>null</tt> using {@link Flob#getMime()}
     * @throws IOException if occur IO errors
     */
    private void writeIntoEpub(Flob file, String path, String id, String mediaType) throws IOException {
        epubWriter.writeToOps(file, path);
        newResource(id, path, (mediaType != null) ? mediaType : file.getMime());
    }
}
