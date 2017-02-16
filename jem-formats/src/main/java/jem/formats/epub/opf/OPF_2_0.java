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

package jem.formats.epub.opf;

import static jem.Attributes.getAuthor;
import static jem.Attributes.getDate;
import static jem.Attributes.getGenre;
import static jem.Attributes.getIntro;
import static jem.Attributes.getKeywords;
import static jem.Attributes.getLanguage;
import static jem.Attributes.getPublisher;
import static jem.Attributes.getRights;
import static jem.Attributes.getTitle;
import static jem.Attributes.getVendor;
import static pw.phylame.commons.util.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import jem.Book;
import jem.epm.util.MakerException;
import jem.epm.util.xml.XmlRender;
import jem.formats.epub.EpubOutConfig;
import jem.formats.epub.OutData;
import jem.formats.epub.item.Guide;
import jem.formats.epub.item.Resource;
import jem.formats.epub.item.Spine;
import lombok.val;
import pw.phylame.commons.util.DateUtils;
import pw.phylame.commons.util.MiscUtils;

/**
 * OPF 2.0 implements.
 */
class OPF_2_0 implements OpfWriter {
    public static final String BOOK_ID_NAME = "book-id";
    public static final String OPF_XML_NS = "http://www.idpf.org/2007/opf";
    public static final String OPF_VERSION_2 = "2.0";

    // DCMI (the Dublin Core Metadata Initiative)
    public static final String DC_XML_NS = "http://purl.org/dc/elements/1.1/";

    public static final String[] OPTIONAL_METADATA = {"source", "relation", "format"};

    private XmlRender render;
    private EpubOutConfig config;

    @Override
    public void write(String coverID,
            List<Resource> resources,
            List<Spine> spines,
            String ncxID,
            List<Guide> guides,
            OutData data) throws IOException, MakerException {
        this.render = data.render;
        this.config = data.config;

        render.beginXml();
        render.beginTag("package").attribute("version", OPF_VERSION_2);
        render.attribute("unique-identifier", BOOK_ID_NAME);
        render.attribute("xmlns", OPF_XML_NS);

        writeMetadata(data.book, coverID);

        // manifest
        render.beginTag("manifest");
        for (val resource : resources) {
            render.beginTag("item")
                    .attribute("id", resource.id)
                    .attribute("href", resource.href)
                    .attribute("media-type", resource.mediaType)
                    .endTag();
        }
        render.endTag();

        // spine
        render.beginTag("spine").attribute("toc", ncxID);
        for (val spine : spines) {
            render.beginTag("itemref").attribute("idref", spine.idref);
            if (!spine.linear) {
                render.attribute("linear", "no");
            }
            if (isNotEmpty(spine.properties)) {
                render.attribute("properties", spine.properties);
            }
            render.endTag();
        }
        render.endTag();

        // guide
        render.beginTag("guide");
        for (val guide : guides) {
            render.beginTag("reference")
                    .attribute("href", guide.href)
                    .attribute("type", guide.type)
                    .attribute("title", guide.title)
                    .endTag();
        }
        render.endTag();

        render.endTag(); // package
        render.endXml();
    }

    private void writeMetadata(Book book, String coverID) throws IOException, MakerException {
        render.beginTag("metadata");
        render.attribute("xmlns:dc", DC_XML_NS).attribute("xmlns:opf", OPF_XML_NS);
        writeDcmi(book, config.uuid, config.uuidType);
        if (isNotEmpty(coverID)) {
            writeMeta("cover", coverID);
        }
        render.endTag();
    }

    private void writeMeta(String name, String content) throws IOException {
        render.beginTag("meta").attribute("name", name).attribute("content", content).endTag();
    }

    private void writeDcmi(Book book, String uuid, String uuidType) throws IOException, MakerException {
        render.beginTag("dc:identifier").attribute("id", BOOK_ID_NAME);
        render.attribute("opf:scheme", uuidType).text(uuid).endTag();

        render.beginTag("dc:title").text(getTitle(book)).endTag();

        String value = getAuthor(book);
        if (isNotEmpty(value)) {
            render.beginTag("dc:creator").attribute("opf:role", "aut").text(value).endTag();
        }

        value = getGenre(book);
        if (isNotEmpty(value)) {
            render.beginTag("dc:type").text(value).endTag();
        }

        value = getKeywords(book);
        if (isNotEmpty(value)) {
            render.beginTag("dc:subject").text(value).endTag();
        }

        val intro = getIntro(book);
        if (intro != null) {
            val text = intro.getText();
            if (isNotEmpty(text)) {
                render.beginTag("dc:description").text(text).endTag();
            }
        }

        value = getPublisher(book);
        if (isNotEmpty(value)) {
            render.beginTag("dc:publisher").text(value).endTag();
        }

        val date = getDate(book);
        if (date != null) {
            render.beginTag("dc:date")
                    .attribute("opf:event", "creation")
                    .text(DateUtils.format(date, config.dateFormat))
                    .endTag();

            val today = new Date();
            if (!today.equals(date)) {
                render.beginTag("dc:date")
                        .attribute("opf:event", "modification")
                        .text(DateUtils.format(today, config.dateFormat))
                        .endTag();
            }
        }

        val language = getLanguage(book);
        if (language != null) {
            render.beginTag("dc:language").text(MiscUtils.renderLocale(language)).endTag();
        }

        value = getRights(book);
        if (isNotEmpty(value)) {
            render.beginTag("dc:rights").text(value).endTag();
        }

        value = getVendor(book);
        if (isNotEmpty(value)) {
            render.beginTag("dc:contributor").attribute("opf:role", "bkp").text(value).endTag();
        }

        val attributes = book.getAttributes();
        for (val key : OPTIONAL_METADATA) {
            value = attributes.get(key, "");
            if (isNotEmpty(value)) {
                render.beginTag("dc:" + key).text(value).endTag();
            }
        }
    }
}
