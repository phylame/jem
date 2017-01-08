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

package pw.phylame.jem.formats.txt;

import pw.phylame.jem.epm.util.config.AbstractConfig;
import pw.phylame.jem.epm.util.config.Configured;
import pw.phylame.jem.formats.util.M;

import java.util.regex.Pattern;

/**
 * Config for parse TXT file.
 */
public class TxtInConfig extends AbstractConfig {
    public static final String SELF = "config";
    public static final String ENCODING = "encoding";
    public static final String PATTERN = "pattern";
    public static final String PATTERN_FLAGS = "patternFlags";
    public static final String TRIM_CHAPTER_TITLE = "trimChapterTitle";
    public static final String BOOK_TITLE = "title";

    /**
     * Text encoding of input file
     */
    @Configured(ENCODING)
    public String encoding = TXT.defaultEncoding;

    /**
     * Chapter title regex pattern
     */
    @Configured(PATTERN)
    public String pattern = M.tr("txt.parse.pattern");

    /**
     * Regex pattern flag.
     */
    @Configured(PATTERN_FLAGS)
    public int patternFlags = Pattern.MULTILINE;

    /**
     * Remove leading and tailing space of chapter title.
     */
    @Configured(TRIM_CHAPTER_TITLE)
    public boolean trimChapterTitle = true;

    /**
     * Title for new book(title of book cannot be parsed from TXT file)
     */
    @Configured(BOOK_TITLE)
    public String title = "";
}
