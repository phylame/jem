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
import lombok.NonNull;
import lombok.val;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Abstract {@code Flob} implementation.
 */
public abstract class AbstractFlob implements Flob {

    @NonNull
    @Getter(lazy = true)
    private final String mime = detectMime();

    protected AbstractFlob(String mime) {
        _mime = mime;
    }

    private final String _mime;

    private String detectMime() {
        return PathUtils.mimeOrDetect(getName(), _mime);
    }

    @Override
    public byte[] readAll() throws IOException {
        try (val in = openStream()) {
            return IOUtils.toBytes(in);
        }
    }

    @Override
    public long writeTo(@NonNull OutputStream out) throws IOException {
        try (val in = openStream()) {
            return IOUtils.copy(in, out, -1);
        }
    }

    @Override
    public String toString() {
        return getName() + ";mime=" + getMime();
    }
}
