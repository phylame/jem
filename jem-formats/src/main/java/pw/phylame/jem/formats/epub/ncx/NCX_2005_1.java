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

import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Jem;
import pw.phylame.jem.epm.util.MakerException;
import pw.phylame.jem.epm.util.xml.XmlRender;
import pw.phylame.jem.formats.epub.*;
import pw.phylame.jem.formats.epub.writer.EpubWriter;
import pw.phylame.ycl.util.StringUtils;

import java.io.IOException;
import java.util.List;

/**
 * NCX version 2005-1
 */
class NCX_2005_1 implements NcxWriter, BookListener {
    public static final String DT_ID = "-//NISO//DTD ncx 2005-1//EN";
    public static final String DT_URI = "http://www.daisy.org/z3986/2005/ncx-2005-1.dtd";

    public static final String VERSION = "2005-1";
    public static final String NAMESPACE = "http://www.daisy.org/z3986/2005/ncx/";

    private int playOrder = 1;

    private XmlRender render;
    private BookRender bookRender;

    @Override
    public void write(EpubWriter writer, OutTuple tuple) throws IOException, MakerException {
        this.render = tuple.render;
        val book = tuple.book;
        val config = tuple.config;
        render.startXml();
        render.docdecl("ncx", DT_ID, DT_URI);

        render.startTag("ncx").attribute("version", VERSION);
        render.attribute("xml:lang", config.htmlConfig.htmlLanguage = EPUB.languageOfBook(book)).attribute("xmlns", NAMESPACE);

        writeHead(Jem.depthOf(book), config.uuid, 0, 0, render);

        // docTitle
        render.startTag("docTitle");
        render.startTag("text").text(Attributes.getTitle(book)).endTag();
        render.endTag();
        // docAuthor
        val author = Attributes.getAuthor(book);
        if (StringUtils.isNotEmpty(author)) {
            render.startTag("docAuthor");
            render.startTag("text").text(author).endTag();
            render.endTag();
        }

        // navMap
        render.startTag("navMap");
        // render contents
        bookRender = new BookRender(writer, this, tuple);
        bookRender.start();

        render.endTag(); // navMap
        render.endTag(); // ncx

        render.endXml();
    }

    @Override
    public String getCoverID() {
        return bookRender.getCoverID();
    }

    @Override
    public List<Resource> getResources() {
        return bookRender.getResources();
    }

    @Override
    public List<Spine> getSpines() {
        return bookRender.getSpineItems();
    }

    @Override
    public List<Guide> getGuides() {
        return bookRender.getGuideItems();
    }

    private void writeHead(int depth, String uuid, int totalPageCount, int maxPageNumber, XmlRender render) throws IOException {
        render.startTag("head");
        writeMeta("dtb:uid", uuid, render);
        writeMeta("dtb:depth", Integer.toString(depth), render);
        writeMeta("dtb:totalPageCount", Integer.toString(totalPageCount), render);
        writeMeta("dtb:maxPageNumber", Integer.toString(maxPageNumber), render);
        render.endTag();
    }

    private void writeMeta(String name, String value, XmlRender render) throws IOException {
        render.startTag("meta").attribute("name", name);
        render.attribute("content", value).endTag();
    }

    @Override
    public void startNavPoint(String id, String href, String title) throws IOException {
        render.startTag("navPoint").attribute("id", id);
        render.attribute("playOrder", Integer.toString(playOrder++));

        render.startTag("navLabel");
        render.startTag("text").text(title).endTag();
        render.endTag(); // navLabel

        render.startTag("content").attribute("src", href).endTag();
    }

    @Override
    public void endNavPoint() throws IOException {
        render.endTag(); // navPoint
    }
}
