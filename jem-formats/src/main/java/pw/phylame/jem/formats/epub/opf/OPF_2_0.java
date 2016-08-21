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

package pw.phylame.jem.formats.epub.opf;

import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.epm.util.MakerException;
import pw.phylame.jem.epm.util.xml.XmlRender;
import pw.phylame.jem.formats.epub.*;
import pw.phylame.ycl.format.Converters;
import pw.phylame.ycl.util.DateUtils;
import pw.phylame.ycl.util.StringUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
                      OutTuple tuple) throws IOException, MakerException {
        this.render = tuple.render;
        this.config = tuple.config;
        render.startXml();
        render.startTag("package").attribute("version", OPF_VERSION_2);
        render.attribute("unique-identifier", BOOK_ID_NAME);
        render.attribute("xmlns", OPF_XML_NS);

        writeMetadata(tuple.book, coverID);

        // manifest
        render.startTag("manifest");
        for (Resource resource : resources) {
            render.startTag("item")
                    .attribute("id", resource.id)
                    .attribute("href", resource.href)
                    .attribute("media-type", resource.mediaType)
                    .endTag();
        }
        render.endTag();

        // spine
        render.startTag("spine").attribute("toc", ncxID);
        for (val item : spines) {
            render.startTag("itemref").attribute("idref", item.idref);
            if (!item.linear) {
                render.attribute("linear", "no");
            }
            if (item.properties != null) {
                render.attribute("properties", item.properties);
            }
            render.endTag();
        }
        render.endTag();
        // guide
        render.startTag("guide");
        for (val item : guides) {
            render.startTag("reference")
                    .attribute("href", item.href)
                    .attribute("type", item.type)
                    .attribute("title", item.title)
                    .endTag();
        }
        render.endTag();

        render.endTag(); // package
        render.endXml();
    }

    private void writeMetadata(Book book, String coverID) throws IOException, MakerException {
        render.startTag("metadata");
        render.attribute("xmlns:dc", DC_XML_NS).attribute("xmlns:opf", OPF_XML_NS);
        addDcmi(book, config.uuid, coverID);
        render.endTag();
    }

    private void addDcmi(Book book, String uuid, String coverID) throws IOException, MakerException {
        render.startTag("dc:identifier").attribute("id", BOOK_ID_NAME);
        render.attribute("opf:scheme", "uuid").text(uuid).endTag();

        render.startTag("dc:title").text(Attributes.getTitle(book)).endTag();

        String value = Attributes.getAuthor(book);
        if (StringUtils.isNotEmpty(value)) {
            render.startTag("dc:creator");
            render.attribute("opf:role", "aut").text(value).endTag();
        }

        value = Attributes.getGenre(book);
        if (StringUtils.isNotEmpty(value)) {
            render.startTag("dc:type").text(value).endTag();
        }

        value = Attributes.getKeywords(book);
        if (StringUtils.isNotEmpty(value)) {
            render.startTag("dc:subject").text(value).endTag();
        }

        val intro = Attributes.getIntro(book);
        if (intro != null) {
            val text = intro.getText();
            if (StringUtils.isNotEmpty(text)) {
                render.startTag("dc:description").text(text).endTag();
            }
        }

        value = Attributes.getPublisher(book);
        if (StringUtils.isNotEmpty(value)) {
            render.startTag("dc:publisher").text(value).endTag();
        }

        if (coverID != null) {
            render.startTag("meta").attribute("name", "cover").attribute("content", coverID).endTag();
        }

        val date = Attributes.getDate(book);
        if (date != null) {
            render.startTag("dc:date")
                    .attribute("opf:event", "creation")
                    .text(DateUtils.format(date, config.dateFormat))
                    .endTag();

            val today = new Date();
            if (!today.equals(date)) {
                render.startTag("dc:date")
                        .attribute("opf:event", "modification")
                        .text(DateUtils.format(today, config.dateFormat))
                        .endTag();
            }
        }

        val language = Attributes.getLanguage(book);
        if (language != null) {
            render.startTag("dc:language").text(Converters.render(language, Locale.class)).endTag();
        }

        value = Attributes.getRights(book);
        if (StringUtils.isNotEmpty(value)) {
            render.startTag("dc:rights").text(value).endTag();
        }

        value = Attributes.getVendor(book);
        if (StringUtils.isNotEmpty(value)) {
            render.startTag("dc:contributor").attribute("opf:role", "bkp").text(value).endTag();
        }

        for (val key : OPTIONAL_METADATA) {
            value = book.getAttributes().get(key, "");
            if (StringUtils.isNotEmpty(value)) {
                render.startTag("dc:" + key).text(value).endTag();
            }
        }
    }
}
