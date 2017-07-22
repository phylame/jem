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

import jclp.vdm.VdmEntry;
import jclp.vdm.VdmReader;
import jem.util.Variants;
import jem.util.flob.AbstractFlob;
import lombok.NonNull;

import java.io.IOException;
import java.io.InputStream;

import static jclp.util.Validate.requireNotNull;

public class VdmFlob extends AbstractFlob {
    static {
        Variants.mapClass(Variants.FLOB, VdmFlob.class);
    }

    private final VdmReader vdmReader;
    private final VdmEntry vdmEntry;

    public VdmFlob(@NonNull VdmReader vdmReader, String name, String mime) {
        super(mime);
        vdmEntry = vdmReader.entryFor(name);
        requireNotNull(vdmEntry, "no such entry %s", name);
        this.vdmReader = vdmReader;
    }

    @Override
    public String getName() {
        return vdmEntry.getName();
    }

    @Override
    public InputStream openStream() throws IOException {
        return vdmReader.streamFor(vdmEntry);
    }
}
