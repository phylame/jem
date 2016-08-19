/*
 * Copyright 2014-2015 Peng Wan <phylame@163.com>
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

package pw.phylame.jem.formats.epub.ncx;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipOutputStream;

import pw.phylame.jem.core.Book;
import pw.phylame.jem.core.Jem;
import pw.phylame.jem.epm.util.MakerException;
import pw.phylame.jem.epm.util.xml.XmlRender;
import pw.phylame.jem.formats.epub.*;
import pw.phylame.jem.formats.epub.writer.EpubWriter;

/**
 * NCX version 2005-1
 */
class NCX_2005_1 implements NcxWriter, ContentsListener {
    public static final String DT_ID = "-//NISO//DTD ncx 2005-1//EN";
    public static final String DT_URI = "http://www.daisy.org/z3986/2005/ncx-2005-1.dtd";

    public static final String VERSION = "2005-1";
    public static final String NAMESPACE = "http://www.daisy.org/z3986/2005/ncx/";

    private int playOrder = 1;

    private XmlRender xmlRender;
    private ContentsRender contentsRender;

    @Override
    public void write(Book book, EpubOutConfig epubConfig, XmlRender render, EpubWriter writer,
                      ZipOutputStream zipout) throws IOException, MakerException {
        this.xmlRender = render;
        render.startXml();
        render.docdecl("ncx", DT_ID, DT_URI);

        render.startTag("ncx").attribute("version", VERSION);
        String lang = EPUB.languageOfBook(book);
        epubConfig.htmlConfig.htmlLanguage = lang;
        render.attribute("xml:lang", lang).attribute("xmlns", NAMESPACE);

        int depth = Jem.depthOf(book);
        writeHead(depth, epubConfig.uuid, 0, 0, render);

        // docTitle
        render.startTag("docTitle");
        render.startTag("text").text(book.getTitle()).endTag();
        render.endTag();
        // docAuthor
        String author = book.getAuthor();
        if (TextUtils.isValid(author)) {
            render.startTag("docAuthor");
            render.startTag("text").text(author).endTag();
            render.endTag();
        }

        // navMap
        render.startTag("navMap");
        // render contents
        contentsRender = new ContentsRender(book, writer, epubConfig, zipout, this);
        contentsRender.start();

        render.endTag(); // navMap
        render.endTag(); // ncx

        render.endXml();
    }

    @Override
    public String getCoverID() {
        return contentsRender.getCoverID();
    }

    @Override
    public List<Resource> getResources() {
        return contentsRender.getResources();
    }

    @Override
    public List<SpineItem> getSpineItems() {
        return contentsRender.getSpineItems();
    }

    @Override
    public List<GuideItem> getGuideItems() {
        return contentsRender.getGuideItems();
    }

    private void writeHead(int depth, String uuid, int totalPageCount, int maxPageNumber,
                           XmlRender xmlRender) throws IOException {
        xmlRender.startTag("head");
        writeMeta("dtb:uid", uuid, xmlRender);
        writeMeta("dtb:depth", Integer.toString(depth), xmlRender);
        writeMeta("dtb:totalPageCount", Integer.toString(totalPageCount), xmlRender);
        writeMeta("dtb:maxPageNumber", Integer.toString(maxPageNumber), xmlRender);
        xmlRender.endTag();
    }

    private void writeMeta(String name, String value, XmlRender xmlRender) throws IOException {
        xmlRender.startTag("meta").attribute("name", name);
        xmlRender.attribute("content", value).endTag();
    }

    @Override
    public void startNavPoint(String id, String href, String title) throws IOException {
        xmlRender.startTag("navPoint").attribute("id", id);
        xmlRender.attribute("playOrder", Integer.toString(playOrder++));

        xmlRender.startTag("navLabel");
        xmlRender.startTag("text").text(title).endTag();
        xmlRender.endTag(); // navLabel

        xmlRender.startTag("content").attribute("src", href).endTag();
    }

    @Override
    public void endNavPoint() throws IOException {
        xmlRender.endTag(); // navPoint
    }
}
