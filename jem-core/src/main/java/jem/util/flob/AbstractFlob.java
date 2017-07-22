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

package jem.util.flob;

import jclp.io.IOUtils;
import jclp.io.PathUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.IOException;
import java.io.OutputStream;

@RequiredArgsConstructor
public abstract class AbstractFlob implements Flob {
    private final String _mime;

    @Getter(lazy = true)
    private final String mime = detectMime();

    private String detectMime() {
        return PathUtils.mimeOrDetect(getName(), _mime);
    }

    @Override
    public long writeTo(OutputStream output) throws IOException {
        try (val input = openStream()) {
            return IOUtils.copy(input, output, -1);
        }
    }

    @Override
    public String toString() {
        return getMime() + ";mime=" + getMime();
    }
}
