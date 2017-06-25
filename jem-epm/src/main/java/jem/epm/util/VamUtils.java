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

import jem.util.flob.Flob;
import jem.util.text.Text;
import lombok.NonNull;
import lombok.val;
import jclp.io.IOUtils;
import jclp.vdm.FileVdmReader;
import jclp.vdm.VdmReader;
import jclp.vdm.VdmWriter;
import jclp.vdm.ZipVdmReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

public final class VamUtils {
    private VamUtils() {
    }

    public static VdmReader openReader(File file, String type) throws IOException {
        switch (type) {
            case "dir":
                return new FileVdmReader(file);
            case "zip":
                return new ZipVdmReader(new ZipFile(file));
            default:
                return null;
        }
    }

    public static VdmReader openReader(File file) throws IOException {
        return file.isDirectory() ? new FileVdmReader(file) : new ZipVdmReader(new ZipFile(file));
    }

    public static InputStream streamOf(@NonNull VdmReader vr, String name) throws IOException {
        val item = vr.entryFor(name);
        if (item == null) {
            throw new IOException(M.tr("err.vam.noEntry", name, vr.getName()));
        }
        return vr.streamOf(item);
    }

    public static String textOf(VdmReader vr, String name, String encoding) throws IOException {
        try (val in = streamOf(vr, name)) {
            return IOUtils.toString(in, encoding);
        }
    }

    public static void write(VdmWriter vw, String name, String str, String encoding) throws IOException {
        val item = vw.newEntry(name);
        vw.begin(item);
        vw.write(item, encoding == null ? str.getBytes() : str.getBytes(encoding));
        vw.end(item);
    }

    public static void write(VdmWriter vw, String name, Flob flob) throws IOException {
        val item = vw.newEntry(name);
        flob.writeTo(vw.begin(item));
        vw.end(item);
    }

    public static void write(VdmWriter vw, String name, Text text, String encoding) throws IOException {
        val item = vw.newEntry(name);
        val writer = IOUtils.writerFor(vw.begin(item), encoding);
        text.writeTo(writer);
        writer.flush();
        vw.end(item);
    }
}
