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

package jem.formats.util;

import jem.epm.util.ParserException;
import lombok.val;
import jclp.text.Converters;
import jclp.util.DateUtils;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

public final class EpmUtils {
    public static Date parseDate(String str, String format) throws ParserException {
        try {
            return DateUtils.parse(str, format);
        } catch (ParseException e) {
            throw new ParserException(M.tr("err.text.invalidDate", str, format), e);
        }
    }

    public static Locale parseLocale(String str) throws ParserException {
        val locale = Converters.parse(str, Locale.class);
        if (locale == null) {
            throw new ParserException(M.tr("err.text.invalidLocale", str));
        } else {
            return locale;
        }
    }
}
