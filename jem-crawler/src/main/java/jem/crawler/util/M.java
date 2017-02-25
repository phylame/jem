/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

package jem.crawler.util;

import lombok.NonNull;
import pw.phylame.commons.util.Linguist;

/**
 * Internal message translator.
 *
 * @author PW[<a href="mailto:phylame@163.com">phylame@163.com</a>]
 */
public final class M {
    private M() {
    }

    private static final String MESSAGES_PATH = "jem/crawler/messages";

    private static final Linguist linguist = new Linguist(MESSAGES_PATH);

    public static String tr(@NonNull String key) {
        return linguist.tr(key);
    }

    public static String tr(@NonNull String key, Object... args) {
        return linguist.tr(key, args);
    }
}
