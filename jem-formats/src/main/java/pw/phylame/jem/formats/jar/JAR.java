/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
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

package pw.phylame.jem.formats.jar;

/**
 * Defines constants and common methods.
 */
public final class JAR {
    static final int MAGIC_NUMBER = 0x13000;

    static final String METADATA_ENCODING = "UTF-8";

    static final String TEXT_ENCODING = "UTF-16LE";

    static final String MANIFEST_FILE = "META-INF/MANIFEST.MF";

    /**
     * Java ME template book file
     */
    static final String JAR_TEMPLATE = "!pw/phylame/jem/formats/jar/book.jar";

    static final String MANIFEST_TEMPLATE = "Manifest-Version: 1.0\n" +
            "Created-By: %s v%s\n" +
            "MIDlet-1: %s,/jm.PNG,JavaBook\n" +
            "MIDlet-Vendor: %s\n" +
            "MIDlet-Version: 1.0\n" +
            "MIDlet-Name: %s\n" +
            "MicroEdition-Configuration: CLDC-1.0\n" +
            "MicroEdition-Profile: MIDP-1.0";
}
