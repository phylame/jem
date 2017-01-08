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

package pw.phylame.jem.util.flob;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FlobWrapper implements Flob {
    @NonNull
    private final Flob flob;

    public final Flob getTarget() {
        return flob;
    }

    @Override
    public String getName() {
        return flob.getMime();
    }

    @Override
    public String getMime() {
        return flob.getMime();
    }

    @Override
    public InputStream openStream() throws IOException {
        return flob.openStream();
    }

    @Override
    public byte[] readAll() throws IOException {
        return flob.readAll();
    }

    @Override
    public long writeTo(OutputStream out) throws IOException {
        return flob.writeTo(out);
    }

}
