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

package jem.util.flob.impl;

import jem.util.Variants;
import jem.util.flob.AbstractFlob;
import lombok.Getter;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class URLFlob extends AbstractFlob {
    static {
        Variants.mapClass(Variants.FLOB, URLFlob.class);
    }

    @Getter
    private final URL url;

    public URLFlob(@NonNull URL url, String mime) {
        super(mime);
        this.url = url;
    }

    @Override
    public String getName() {
        return url.getPath();
    }

    @Override
    public InputStream openStream() throws IOException {
        return url.openStream();
    }

    @Override
    public String toString() {
        return url.toString() + ";mime=" + getMime();
    }
}
