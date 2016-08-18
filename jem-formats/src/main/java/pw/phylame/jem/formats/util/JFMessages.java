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

package pw.phylame.jem.formats.util;

import pw.phylame.ycl.util.Provider;
import pw.phylame.ycl.util.Translator;
import pw.phylame.ycl.value.Lazy;

public final class JFMessages {
    private JFMessages() {
    }

    private static final String MESSAGES_PATH = "pw/phylame/jem/formats/messages.properties";

    private static final Lazy<Translator> translator = new Lazy<>(new Provider<Translator>() {
        @Override
        public Translator provide() throws Exception {
            return new Translator(MESSAGES_PATH, null);
        }
    });

    public static String tr(String key) {
        return translator.get().tr(key);
    }

    public static String tr(String key, Object... args) {
        return translator.get().tr(key, args);
    }
}
