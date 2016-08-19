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

import pw.phylame.jem.core.Book;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.formats.epub.writer.EpubWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes book HTML tree.
 */
public class ContentsRender {
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
    private final ContentsListener contentsListener;

    public ContentsRender(Book book, EpubWriter epubWriter,
                          EpubOutConfig epubConfig, ZipOutputStream zipout,
                          ContentsListener contentsListener) {
        this.book = book;
        this.epubWriter = epubWriter;
        this.epubConfig = epubConfig;
        this.zipout = zipout;
        this.contentsListener = contentsListener;
    }

    private String coverID;
    private final List<Resource> resources = new LinkedList<>();
    private final List<SpineItem> spines = new LinkedList<>();
    private final List<GuideItem> guides = new LinkedList<>();

    private void newResource(String id, String href, String mime) {
        resources.add(new Resource(id, href, mime));
    }

    private void newGuideItem(String href, String type, String title) {
        guides.add(new GuideItem(href, type, title));
    }

    private void newSpineItem(String id, boolean linear, String properties) {
        spines.add(new SpineItem(id, linear, properties));
    }

    private void newNaviItem(String id, String href, String title, String properties)
            throws IOException {
        newSpineItem(id, true, properties);
        contentsListener.startNavPoint(id, href, title);
    }

    private void endNaviItem() throws IOException {
        contentsListener.endNavPoint();
    }

    public String getCoverID() {
        return coverID;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public List<SpineItem> getSpineItems() {
        return spines;
    }

    public List<GuideItem> getGuideItems() {
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
        Flob cover = book.getCover();
        if (cover == null) {    // no cover write intro
            writeIntroPage();
        } else {
            String title = MessageBundle.getText("epub.page.cover.title");
            coverID = COVER_NAME + "-image";
            String href = writeImage(cover, COVER_NAME, coverID);
            if (epubConfig.smallPage) {   // for phone split to cover and intro page
                writeCoverPage(title, href);
                writeIntroPage();
            } else {    // one page
                writeCoverIntro(title, href);
            }
        }
    }

    private void writeCoverPage(String title, String coverHref)
            throws IOException {
        StringWriter writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderCover(title, coverHref, title);
        String href = writeText(writer.toString(), COVER_NAME, hrefOfText(COVER_NAME));
        newSpineItem(COVER_NAME, true, DUOKAN_FULL_SCREEN);
        newGuideItem(href, "cover", title);
    }

    private void writeIntroPage() throws IOException {
        Text intro = book.getIntro();
        if (intro == null) {    // no book intro
            return;
        }
        String title = MessageBundle.getText("epub.page.intro.title");
        String baseName = INTRO_NAME;
        StringWriter writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderIntro(title, book.getTitle(), title, intro);
        String href = writeText(writer.toString(), baseName, hrefOfText(baseName));
        newNaviItem(baseName, href, title, null);
        endNaviItem();
    }

    private void writeCoverIntro(String title, String coverHref) throws IOException {
        StringWriter writer = new StringWriter();
        htmlRender.setOutput(writer);
        Text intro = book.getIntro();
        if (intro != null) {
            String bookTitle = book.getTitle();
            htmlRender.renderCoverIntro(title, coverHref, bookTitle, bookTitle,
                    MessageBundle.getText("epub.page.intro.title"), intro);
        } else {
            htmlRender.renderCover(title, coverHref, title);
        }
        String href = writeText(writer.toString(), COVER_NAME, hrefOfText(COVER_NAME));
        newNaviItem(COVER_NAME, href, title, DUOKAN_FULL_SCREEN);
        endNaviItem();
        newGuideItem(href, "cover", title);
    }

    private void writeToc() throws IOException {
        String title = MessageBundle.getText("epub.page.toc.title");
        String href = TEXT_DIR + "/" + hrefOfText(TOC_NAME);
        newNaviItem(TOC_NAME, href, title, null);
        endNaviItem();
        // sections and chapters
        List<HtmlRender.Link> links = processSection(book, "", hrefOfText(TOC_NAME));
        StringWriter writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderToc(title, links);
        writeText(writer.toString(), TOC_NAME, hrefOfText(TOC_NAME));
        newGuideItem(href, "toc", title);
    }

    // return links of sub-chapters
    private List<HtmlRender.Link> processSection(Chapter section, String suffix,
                                                 String myHref) throws IOException {
        List<HtmlRender.Link> links = new LinkedList<>();
        HtmlRender.Link link;
        String mySuffix;
        int count = 1;
        for (Chapter sub : section) {
            mySuffix = suffix + "-" + Integer.toString(count);
            if (!sub.isSection()) {
                link = writeChapter(sub, mySuffix);
            } else {
                link = writeSection(sub, mySuffix, myHref);
            }
            links.add(link);
            ++count;
        }
        return links;
    }

    private HtmlRender.Link writeSection(Chapter section, String suffix,
                                         String parentHref) throws IOException {
        String baseName = "section" + suffix;
        String name = hrefOfText(baseName);
        String href = TEXT_DIR + "/" + name;
        String sectionTitle = section.getTitle();

        String coverHref = writePartCover(section, baseName);
        if (coverHref != null && epubConfig.smallPage) {
            writeSectionCover(sectionTitle, coverHref, baseName);
            coverHref = null;
        }
        newNaviItem(baseName, href, sectionTitle, null);

        // sub-chapters
        String myHref = hrefOfText("section" + suffix);
        List<HtmlRender.Link> links = processSection(section, suffix, myHref);
        if (parentHref != null) {
            String title = MessageBundle.getText("epub.page.contents.gotoTop");
            links.add(new HtmlRender.Link(title, parentHref));
        }

        StringWriter writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderSection(sectionTitle, coverHref, sectionTitle,
                section.getIntro(), links);
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
        String baseName = "chapter" + suffix;
        String name = hrefOfText(baseName);
        String href = TEXT_DIR + "/" + name;
        String chapterTitle = chapter.getTitle();

        Text content = chapter.getContent();
        if (content != null && content.getType().equals(Text.HTML)) {    // content already HTML
            href = writeText(content, baseName, name);
            newNaviItem(baseName, href, chapterTitle, null);
            endNaviItem();
            return new HtmlRender.Link(chapterTitle, name);
        }

        String coverHref = writePartCover(chapter, baseName);
        if (coverHref != null && epubConfig.smallPage) {
            writeChapterCover(chapterTitle, coverHref, baseName);
            coverHref = null;
        }

        ZipEntry zipEntry = new ZipEntry(epubWriter.pathInOps(href));
        zipout.putNextEntry(zipEntry);
        htmlRender.setOutput(zipout);
        htmlRender.renderChapter(chapterTitle, coverHref, chapterTitle,
                chapter.getIntro(), content != null ? content : Texts.emptyText());
        zipout.closeEntry();

        newResource(baseName, href, MT_XHTML);
        newNaviItem(baseName, href, chapterTitle, null);
        endNaviItem();
        return new HtmlRender.Link(chapterTitle, name);
    }

    private void writeChapterCover(String title, String coverHref, String baseName)
            throws IOException {
        String id = baseName + "-" + COVER_NAME;
        StringWriter writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderChapterCover(title, coverHref, title);
        writeText(writer.toString(), id, hrefOfText(id));
        newSpineItem(id, true, DUOKAN_FULL_SCREEN);
        endNaviItem();
    }

    private void writeSectionCover(String title, String coverHref, String baseName)
            throws IOException {
        String id = baseName + "-" + COVER_NAME;
        StringWriter writer = new StringWriter();
        htmlRender.setOutput(writer);
        htmlRender.renderSectionCover(title, coverHref, title);
        writeText(writer.toString(), id, hrefOfText(id));
        newSpineItem(id, true, DUOKAN_FULL_SCREEN);
    }

    private String writePartCover(Chapter chapter, String baseName) throws IOException {
        Flob cover = chapter.getCover();
        if (cover == null) {
            return null;
        }
        String name = baseName + "-" + COVER_NAME;
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
        String path = IMAGE_DIR + "/" + name + "." + IOUtils.getExtension(file.getName());
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
        String path = TEXT_DIR + "/" + name;
        epubWriter.writeIntoOps(text, path, epubConfig.htmlConfig.encoding);
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
        String path = TEXT_DIR + "/" + name;
        epubWriter.writeIntoOps(text, path, epubConfig.htmlConfig.encoding);
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
        String name = STYLE_DIR + "/" + CSS_FILE;
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
    private void writeIntoEpub(Flob file, String path, String id,
                               String mediaType) throws IOException {
        epubWriter.writeIntoOps(file, path);
        newResource(id, path, (mediaType != null) ? mediaType : file.getMime());
    }
}
