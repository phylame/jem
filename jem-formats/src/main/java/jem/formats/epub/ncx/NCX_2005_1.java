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

package jem.formats.epub.ncx;

import jem.Attributes;
import jem.Chapter;
import jem.epm.util.MakerException;
import jem.epm.util.xml.XmlRender;
import jem.formats.epub.EPUB;
import jem.formats.epub.OutData;
import jem.formats.epub.TocListener;
import jem.formats.epub.TocWriter;
import jem.formats.epub.item.Guide;
import jem.formats.epub.item.Resource;
import jem.formats.epub.item.Spine;
import jem.formats.epub.writer.EpubWriter;
import lombok.val;
import pw.phylame.commons.util.MiscUtils;
import pw.phylame.commons.util.StringUtils;

import java.io.IOException;
import java.util.List;

/**
 * NCX version 2005-1
 */
class NCX_2005_1 implements NcxWriter, TocListener {
    public static final String DT_ID = "-//NISO//DTD ncx 2005-1//EN";
    public static final String DT_URI = "http://www.daisy.org/z3986/2005/ncx-2005-1.dtd";

    public static final String VERSION = "2005-1";
    public static final String NAMESPACE = "http://www.daisy.org/z3986/2005/ncx/";

    private int playOrder = 1;

    private XmlRender render;
    private TocWriter tocWriter;

    @Override
    public void write(EpubWriter writer, OutData data) throws IOException, MakerException {
        this.render = data.render;
        val book = data.book;
        val config = data.config;
        render.beginXml();
        render.docdecl("ncx", DT_ID, DT_URI);

        render.beginTag("ncx")
                .attribute("version", VERSION)
                .attribute("xml:lang", config.htmlConfig.htmlLanguage = EPUB.languageOfBook(book))
                .attribute("xmlns", NAMESPACE);

        writeHead(MiscUtils.depthOf((Chapter) book), config.uuid, 0, 0);

        // docTitle
        render.beginTag("docTitle");
        render.beginTag("text").text(Attributes.getTitle(book)).endTag();
        render.endTag();
        // docAuthor
        val author = Attributes.getAuthor(book);
        if (StringUtils.isNotEmpty(author)) {
            render.beginTag("docAuthor");
            render.beginTag("text").text(author).endTag();
            render.endTag();
        }

        // navMap
        render.beginTag("navMap");
        // render contents
        tocWriter = new TocWriter(writer, this, data);
        tocWriter.start();

        render.endTag(); // navMap
        render.endTag(); // ncx

        render.endXml();
    }

    @Override
    public String getCoverId() {
        return tocWriter.getCoverId();
    }

    @Override
    public List<Resource> getResources() {
        return tocWriter.getResources();
    }

    @Override
    public List<Spine> getSpines() {
        return tocWriter.getSpineItems();
    }

    @Override
    public List<Guide> getGuides() {
        return tocWriter.getGuideItems();
    }

    private void writeHead(int depth, String uuid, int totalPageCount, int maxPageNumber) throws IOException {
        render.beginTag("head");
        writeMeta("dtb:uid", uuid);
        writeMeta("dtb:depth", Integer.toString(depth));
        writeMeta("dtb:totalPageCount", Integer.toString(totalPageCount));
        writeMeta("dtb:maxPageNumber", Integer.toString(maxPageNumber));
        render.endTag();
    }

    private void writeMeta(String name, String value) throws IOException {
        render.beginTag("meta")
                .attribute("name", name)
                .attribute("content", value)
                .endTag();
    }

    @Override
    public void beginNavPoint(String id, String href, String title) throws IOException {
        render.beginTag("navPoint").attribute("id", id);
        render.attribute("playOrder", Integer.toString(playOrder++));

        render.beginTag("navLabel");
        render.beginTag("text").text(title).endTag();
        render.endTag(); // navLabel

        render.beginTag("content").attribute("src", href).endTag();
    }

    @Override
    public void endNavPoint() throws IOException {
        render.endTag(); // navPoint
    }
}
