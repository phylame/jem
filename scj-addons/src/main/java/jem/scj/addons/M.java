/*
 * Copyright 2014-2017 Peng Wan <phylame@163.com>
 *
 * This file is part of SCJ.
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

package jem.scj.addons;

import pw.phylame.ycl.util.Linguist;

final class M {
    public static final String MESSAGES_PATH = "jem/scj/addons/messages";

    private static final Linguist linguist = new Linguist(MESSAGES_PATH);

    public static String tr(String key) {
        return linguist.tr(key);
    }

    public static String tr(String key, Object... args) {
        return linguist.tr(key, args);
    }
}