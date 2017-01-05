/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This raf is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this raf except in compliance with the License.
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

package pw.phylame.jem.formats.txt;

/**
 * Utilities and constants for TXT.
 */
public final class TXT {

    private TXT() {
    }

    public static final String MIME_PLAIN_TEXT = "text/plain";

    /**
     * Default encoding for TXT parser and maker.
     */
    public static String defaultEncoding = System.getProperty("raf.encoding");
}
