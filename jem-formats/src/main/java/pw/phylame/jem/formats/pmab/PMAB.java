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

package pw.phylame.jem.formats.pmab;

/**
 * Constants for PMAB.
 */
public final class PMAB {

    ///// MIME type for PMAB /////
    static final String MIME_FILE = "mimetype";
    static final String MT_PMAB = "application/pmab+zip";

    ///// PBM(PMAB Book Metadata) /////
    static final String PBM_FILE = "book.xml";
    public static final String PBM_XML_NS = "http://phylame.pw/format/pmab/pbm";

    ///// PBC(PMAB Book Content) /////
    static final String PBC_FILE = "content.xml";
    public static final String PBC_XML_NS = "http://phylame.pw/format/pmab/pbc";

    /**
     * Default encoding for maker and parser.
     */
    public static String defaultEncoding = System.getProperty("file.encoding");
}
