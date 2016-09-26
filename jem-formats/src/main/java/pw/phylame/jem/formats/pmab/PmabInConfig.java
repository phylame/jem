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

package pw.phylame.jem.formats.pmab;

import pw.phylame.jem.epm.base.ZipInConfig;
import pw.phylame.jem.epm.util.config.Configured;

/**
 * Config for parse PMAB file.
 */
public class PmabInConfig extends ZipInConfig {
    public static final String SELF = "config";
    public static final String TEXT_ENCODING = "textEncoding";
    public static final String USE_CHAPTER_ENCODING = "useChapterEncoding";
    public static final String DATE_FORMAT = "dateFormat";

    /**
     * default encoding for chapter and intro text
     */
    @Configured(TEXT_ENCODING)
    public String textEncoding = PMAB.defaultEncoding;

    /**
     * PMAB 2: when intro encoding is not existed, use chapter encoding
     */
    @Configured(USE_CHAPTER_ENCODING)
    public boolean useChapterEncoding = true;

    /**
     * default date format if the format in PMAB is unknown
     **/
    @Configured(DATE_FORMAT)
    public String dateFormat = "yyyy-M-d H:m:S";
}
