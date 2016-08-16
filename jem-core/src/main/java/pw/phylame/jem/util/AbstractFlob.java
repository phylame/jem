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

package pw.phylame.jem.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import pw.phylame.ycl.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstract <code>Flob</code> implementation.
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractFlob implements Flob {
    @Getter
    @NonNull
    private final String mime;

    @Override
    public byte[] readAll() throws IOException {
        try (InputStream in = openStream()) {
            return IOUtils.toBytes(in);
        }
    }

    @Override
    public int writeTo(@NonNull OutputStream out) throws IOException {
        try (InputStream in = openStream()) {
            return IOUtils.copy(in, out, -1);
        }
    }

    @Override
    public String toString() {
        return getName() + ";mime=" + getMime();
    }
}
