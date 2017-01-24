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

package jem.formats.epub;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jem.core.Attributes;
import jem.core.Book;
import jem.core.Chapter;
import jem.epm.util.MakerException;
import jem.formats.epub.item.Guide;
import jem.formats.epub.item.Resource;
import jem.formats.epub.item.Spine;
import jem.formats.epub.writer.EpubWriter;
import jem.formats.util.M;
import jem.formats.util.html.HtmlRender;
import jem.formats.util.html.StyleProvider;
import jem.util.flob.Flob;
import jem.util.text.Text;
import jem.util.text.Texts;
import lombok.val;
import pw.phylame.commons.io.PathUtils;

/**
 * Writes book HTML tree.
 */
public class TocWriter {
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
    private final TocListener bookListener;

    public TocWriter(EpubWriter epubWriter, TocListener bookListener, OutData data) {
        this.book = data.book;
        this.epubWriter = epubWriter;
        this.epubConfig = data.config;
        this.zipout = data.zipout;
        this.bookListener = bookListener;
    }

    private String coverId;
    private final List<Resource> resources = new LinkedList<>();
    private final List<Spine> spines = new LinkedList<>();
    private final List<Guide> guides = new LinkedList<>();

    private void newResource(String id, String href, String mime) {
        resources.add(new Resource(id, href, mime));
    }

    private void newGuide(String href, String type, String title) {
        guides.add(new Guide(href, type, title));
    }

    private void newSpine(String id, boolean linear, String properties) {
        spines.add(new Spine(id, linear, properties));
    }

    private void beginNavItem(String id, String href, String title, String properties) throws IOException {
        newSpine(id, true, properties);
        bookListener.beginNavPoint(id, href, title);
    }

    private void endNavItem() throws IOException {
        bookListener.endNavPoint();
    }

    public String getCoverId() {
        return coverId;
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
        if (!book.isSection()) { // no sub-chapter
            return;
        }
        writeCss();
        htmlRender = new HtmlRender(epubConfig.htmlConfig);
        writeCover();
        writeToc();
    }

    private void writeCover() throws IOException {
        val cover = Attributes.getCover(book);
        if (cover == null) { // no cover write intro
            writeIntroPage();
        } else {
            val title = M.tr("epub.page.cover.title");
            coverId = COVER_NAME + "-image";
            val href = writeImage(cover, COVER_NAME, coverId);
            if (epubConfig.smallPage) { // for phone split to cover and intro pages
                writeCoverPage(title, href);
                writeIntroPage();
            } else { // one page
                writeCoverIntro(title, href);
            }
        }
    }

    private void writeCoverPage(String title, String coverHref) throws IOException {
        val writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderCover(title, coverHref, title);
        val href = writeText(writer.toString(), COVER_NAME, hrefOfText(COVER_NAME));
        newSpine(COVER_NAME, true, DUOKAN_FULL_SCREEN);
        newGuide(href, "cover", title);
    }

    private void writeIntroPage() throws IOException {
        val intro = Attributes.getIntro(book);
        if (intro == null) { // no book intro
            return;
        }
        val title = M.tr("epub.page.intro.title");
        val baseName = INTRO_NAME;
        val writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderIntro(title, Attributes.getTitle(book), title, intro);
        val href = writeText(writer.toString(), baseName, hrefOfText(baseName));
        beginNavItem(baseName, href, title, null);
        endNavItem();
    }

    private void writeCoverIntro(String title, String coverHref) throws IOException {
        val writer = new StringWriter();
        htmlRender.setOutput(writer);
        Text intro = Attributes.getIntro(book);
        if (intro != null) {
            val bookTitle = Attributes.getTitle(book);
            htmlRender.renderCoverIntro(title, coverHref, bookTitle, bookTitle, M.tr("epub.page.intro.title"), intro);
        } else {
            htmlRender.renderCover(title, coverHref, title);
        }
        val href = writeText(writer.toString(), COVER_NAME, hrefOfText(COVER_NAME));
        beginNavItem(COVER_NAME, href, title, DUOKAN_FULL_SCREEN);
        endNavItem();
        newGuide(href, "cover", title);
    }

    private void writeToc() throws IOException {
        val title = M.tr("epub.page.toc.title");
        val href = TEXT_DIR + "/" + hrefOfText(TOC_NAME);
        beginNavItem(TOC_NAME, href, title, null);
        endNavItem();
        // sections and chapters
        val links = processSection(book, "", hrefOfText(TOC_NAME));
        val writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderToc(title, links);
        writeText(writer.toString(), TOC_NAME, hrefOfText(TOC_NAME));
        newGuide(href, "toc", title);
    }

    // return links of sub-chapters
    private List<HtmlRender.Link> processSection(Chapter section, String suffix, String href) throws IOException {
        val links = new LinkedList<HtmlRender.Link>();
        String _suffix;
        int count = 1;
        for (val chapter : section) {
            _suffix = suffix + "-" + Integer.toString(count);
            links.add(!chapter.isSection() ? writeChapter(chapter, _suffix) : writeSection(chapter, _suffix, href));
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
        beginNavItem(baseName, href, sectionTitle, null);

        // sub-chapters
        val myHref = hrefOfText("section" + suffix);
        val links = processSection(section, suffix, myHref);
        if (parentHref != null) {
            links.add(new HtmlRender.Link(M.tr("epub.page.contents.gotoTop"), parentHref));
        }

        val writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderSection(sectionTitle, coverHref, sectionTitle, Attributes.getIntro(section), links);
        writeText(writer.toString(), baseName, name);
        endNavItem();
        return new HtmlRender.Link(sectionTitle, name);
    }

    /**
     * Writes specified chapter to OPS text directory.
     *
     * @param chapter
     *            the chapter
     * @param suffix
     *            suffix string for file path
     * @return link to the chapter HTML, relative to HTML in textDir
     * @throws IOException
     *             if occur IO errors
     */
    private HtmlRender.Link writeChapter(Chapter chapter, String suffix) throws IOException {
        val baseName = "chapter" + suffix;
        val name = hrefOfText(baseName);
        val href = TEXT_DIR + "/" + name;
        val chapterTitle = Attributes.getTitle(chapter);

        val text = chapter.getText();
        if (text != null && text.getType().equals(Texts.HTML)) { // text already HTML
            beginNavItem(baseName, writeText(text, baseName, name), chapterTitle, null);
            endNavItem();
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
                : Texts.forEmpty(Texts.PLAIN));
        zipout.closeEntry();

        newResource(baseName, href, MT_XHTML);
        beginNavItem(baseName, href, chapterTitle, null);
        endNavItem();
        return new HtmlRender.Link(chapterTitle, name);
    }

    private void writeChapterCover(String title, String coverHref, String baseName) throws IOException {
        val id = baseName + "-" + COVER_NAME;
        val writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderChapterCover(title, coverHref, title);
        writeText(writer.toString(), id, hrefOfText(id));
        newSpine(id, true, DUOKAN_FULL_SCREEN);
        endNavItem();
    }

    private void writeSectionCover(String title, String coverHref, String baseName) throws IOException {
        val id = baseName + "-" + COVER_NAME;
        val writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderSectionCover(title, coverHref, title);
        writeText(writer.toString(), id, hrefOfText(id));
        newSpine(id, true, DUOKAN_FULL_SCREEN);
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
     * @param file
     *            the image file
     * @param name
     *            base name of image (no extension)
     * @return path relative to HTML in textDir
     * @throws IOException
     *             if occur IO errors
     */
    private String writeImage(Flob file, String name, String id) throws IOException {
        val path = IMAGE_DIR + "/" + name + "." + PathUtils.extName(file.getName());
        writeIntoEpub(file, path, id, file.getMime());
        return "../" + path; // relative to HTML in textDir
    }

    private String hrefOfText(String baseName) {
        return baseName + ".xhtml";
    }

    /**
     * Writes specified HTML text to OPS text directory.
     *
     * @param text
     *            the HTML string
     * @param id
     *            id of the HTML file
     * @param name
     *            name of HTML file
     * @return path in OPS
     * @throws IOException
     *             if occur IO errors
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
     * @param text
     *            the <tt>Text</tt> containing HTML
     * @param id
     *            id of the HTML file
     * @param name
     *            name of HTML
     * @return path in OPS
     * @throws IOException
     *             if occur IO errors
     */
    private String writeText(Text text, String id, String name) throws IOException {
        val path = TEXT_DIR + "/" + name;
        epubWriter.writeToOps(text, path, epubConfig.htmlConfig.encoding);
        newResource(id, path, MT_XHTML);
        return path;
    }

    /**
     * Writes CSS file of HtmlConfig to OPS style directory.
     * <p>
     * After writing, the cssHref of HtmlConfig will be assigned.
     *
     * @throws IOException
     *             if occur IO errors
     */
    private void writeCss() throws IOException {
        if (epubConfig.htmlConfig.style == null) {
            epubConfig.htmlConfig.style = StyleProvider.getDefaults();
        }
        val name = STYLE_DIR + "/" + CSS_FILE;
        writeIntoEpub(epubConfig.htmlConfig.style.cssFile, name, CSS_FILE_ID, MT_CSS);
        epubConfig.htmlConfig.cssHref = "../" + name; // relative to HTML in textDir
    }

    /**
     * Writes file in OPS to ePub archive.
     *
     * @param file
     *            the file
     * @param path
     *            path in ops
     * @param id
     *            id of the file
     * @param mediaType
     *            media type of the file, if <tt>null</tt> using {@link Flob#getMime()}
     * @throws IOException
     *             if occur IO errors
     */
    private void writeIntoEpub(Flob file, String path, String id, String mediaType) throws IOException {
        epubWriter.writeToOps(file, path);
        newResource(id, path, (mediaType != null) ? mediaType : file.getMime());
    }
}
