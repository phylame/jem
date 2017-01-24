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

package jem.epm.util;

import jem.core.Chapter;
import lombok.RequiredArgsConstructor;
import pw.phylame.commons.function.Consumer;
import pw.phylame.commons.io.IOUtils;

import java.io.Closeable;

@RequiredArgsConstructor
public class InputCleaner implements Consumer<Chapter> {
    private final Closeable in;

    private final Runnable addon;

    public InputCleaner(Closeable in) {
        this(in, null);
    }

    @Override
    public void consume(Chapter chapter) {
        IOUtils.closeQuietly(in);
        if (addon != null) {
            addon.run();
        }
    }

}
