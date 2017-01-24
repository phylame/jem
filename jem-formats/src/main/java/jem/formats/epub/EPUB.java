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

import java.util.Locale;

import jem.core.Attributes;
import jem.core.Book;

/**
 * Constants and utilities for ePub.
 */
public class EPUB {
    ///// MIME type for ePub /////
    public static final String MIME_FILE = "mimetype";

    // container.xml
    public static final String CONTAINER_FILE = "META-INF/container.xml";

    // OPF (the Open Packaging Format)
    public static final String OPF_FILE = "content.opf";

    // DCMI (the Dublin Core Metadata Initiative)
    public static final String DC_XML_NS = "http://purl.org/dc/elements/1.1/";

    // NCX (the Navigation Center eXtended)
    public static final String NCX_FILE = "toc.ncx";
    public static final String NCX_FILE_ID = "ncx";

    public static final String BOOK_ID_NAME = "book_id";

    // required media type
    public static final String MT_EPUB = "application/epub+zip";
    public static final String MT_OPF = "application/oebps-package+xml";
    public static final String MT_NCX = "application/x-dtbncx+xml";
    public static final String MT_CSS = "text/css";
    public static final String MT_XHTML = "application/xhtml+xml";

    // cover image
    public static final String COVER_NAME = "cover";
    public static final String COVER_FILE_ID = "cover-image";

    // main CSS
    public static final String CSS_FILE = "style.css";
    public static final String CSS_FILE_ID = "main-css";

    // for DuoKan reader full screen image
    public static final String DUOKAN_FULL_SCREEN = "duokan-page-fullscreen";

    // content
    public static final String COVER_PAGE_ID = "cover-page";
    public static final String COVER_PAGE_FILE = "cover.xhtml";

    public static final String INTRO_PAGE_ID = "intro-page";
    public static final String INTRO_PAGE_FILE = "intro.xhtml";

    public static final String INFO_PAGE_ID = "info-page";
    public static final String INFO_PAGE_FILE = "info.xhtml";

    public static final String TOC_PAGE_ID = "toc-page";
    public static final String TOC_PAGE_FILE = "toc.xhtml";

    public static String dateFormat = "yyyy-M-d";

    public static String languageOfBook(Book book) {
        Locale locale = Attributes.getLanguage(book);
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return locale.getLanguage() + "-" + locale.getCountry();
    }
}
