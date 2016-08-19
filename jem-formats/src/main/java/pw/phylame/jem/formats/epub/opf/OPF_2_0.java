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

import pw.phylame.jem.core.Book;
import pw.phylame.jem.formats.epub.*;
import pw.phylame.jem.formats.util.MakerException;
import pw.phylame.jem.formats.util.text.TextUtils;
import pw.phylame.jem.formats.util.xml.XmlRender;
import pw.phylame.jem.util.Text;

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


    private XmlRender xmlRender;
    private EpubOutConfig epubConfig;

    @Override
    public void write(Book book, EpubOutConfig epubConfig, XmlRender xmlRender,
                      String coverID, List<Resource> resources,
                      List<SpineItem> spineItems, String ncxID, List<GuideItem> guideItems)
            throws IOException, MakerException {
        this.xmlRender = xmlRender;
        this.epubConfig = epubConfig;
        xmlRender.startXml();
        xmlRender.startTag("package").attribute("version", OPF_VERSION_2);
        xmlRender.attribute("unique-identifier", BOOK_ID_NAME);
        xmlRender.attribute("xmlns", OPF_XML_NS);

        writeMetadata(book, coverID);

        // manifest
        xmlRender.startTag("manifest");
        for (Resource resource : resources) {
            xmlRender.startTag("item").attribute("id", resource.id);
            xmlRender.attribute("href", resource.href);
            xmlRender.attribute("media-type", resource.mediaType);
            xmlRender.endTag();
        }
        xmlRender.endTag();

        // spine
        xmlRender.startTag("spine").attribute("toc", ncxID);
        for (SpineItem item : spineItems) {
            xmlRender.startTag("itemref").attribute("idref", item.idref);
            if (!item.linear) {
                xmlRender.attribute("linear", "no");
            }
            if (item.properties != null) {
                xmlRender.attribute("properties", item.properties);
            }
            xmlRender.endTag();
        }
        xmlRender.endTag();
        // guide
        xmlRender.startTag("guide");
        for (GuideItem item : guideItems) {
            xmlRender.startTag("reference").attribute("href", item.href);
            xmlRender.attribute("type", item.type);
            xmlRender.attribute("title", item.title).endTag();
        }
        xmlRender.endTag();

        xmlRender.endTag(); // package
        xmlRender.endXml();
    }

    private void writeMetadata(Book book, String coverID) throws IOException, MakerException {
        xmlRender.startTag("metadata");
        xmlRender.attribute("xmlns:dc", DC_XML_NS).attribute("xmlns:opf", OPF_XML_NS);
        addDcmi(book, epubConfig.uuid, coverID);
        xmlRender.endTag();
    }

    private void addDcmi(Book book, String uuid, String coverID) throws IOException, MakerException {
        xmlRender.startTag("dc:identifier").attribute("id", BOOK_ID_NAME);
        xmlRender.attribute("opf:scheme", "uuid").text(uuid).endTag();

        xmlRender.startTag("dc:title").text(book.getTitle()).endTag();

        String str = book.getAuthor();
        if (!str.isEmpty()) {
            xmlRender.startTag("dc:creator");
            xmlRender.attribute("opf:role", "aut").text(str).endTag();
        }

        str = book.getGenre();
        if (!str.isEmpty()) {
            xmlRender.startTag("dc:type").text(str).endTag();
        }

        str = book.getSubject();
        if (!str.isEmpty()) {
            xmlRender.startTag("dc:subject").text(str).endTag();
        }

        Text intro = book.getIntro();
        if (intro != null) {
            String text = TextUtils.fetchText(intro, null);
            if (TextUtils.isValid(text)) {
                xmlRender.startTag("dc:description").text(text).endTag();
            }
        }

        str = book.getPublisher();
        if (!str.isEmpty()) {
            xmlRender.startTag("dc:publisher").text(str).endTag();
        }

        if (coverID != null) {
            xmlRender.startTag("meta").attribute("name", "cover");
            xmlRender.attribute("content", coverID).endTag();
        }

        Date date = book.getDate();
        if (date != null) {
            xmlRender.startTag("dc:date").attribute("opf:event", "creation");
            xmlRender.text(TextUtils.formatDate(date, epubConfig.dateFormat));
            xmlRender.endTag();

            Date today = new Date();
            if (!today.equals(date)) {
                xmlRender.startTag("dc:date").attribute("opf:event", "modification");
                xmlRender.text(TextUtils.formatDate(today, epubConfig.dateFormat));
                xmlRender.endTag();
            }
        }

        Locale locale = book.getLanguage();
        if (locale != null) {
            xmlRender.startTag("dc:language").text(TextUtils.formatLocale(locale)).endTag();
        }

        str = book.getRights();
        if (!str.isEmpty()) {
            xmlRender.startTag("dc:rights").text(str).endTag();
        }

        str = book.getVendor();
        if (!str.isEmpty()) {
            xmlRender.startTag("dc:contributor").attribute("opf:role", "bkp");
            xmlRender.text(str).endTag();
        }

        for (String key : OPTIONAL_METADATA) {
            str = book.stringAttribute(key, "");
            if (!str.isEmpty()) {
                xmlRender.startTag("dc:" + key).text(str).endTag();
            }
        }
    }
}
