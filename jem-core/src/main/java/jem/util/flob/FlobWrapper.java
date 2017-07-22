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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RequiredArgsConstructor
public class FlobWrapper implements Flob {
    @NonNull
    private final Flob actual;

    @Override
    public String getName() {
        return actual.getName();
    }

    @Override
    public String getMime() {
        return actual.getMime();
    }

    @Override
    public InputStream openStream() throws IOException {
        return actual.openStream();
    }

    @Override
    public long writeTo(OutputStream output) throws IOException {
        return actual.writeTo(output);
    }
}
