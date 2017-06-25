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

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Utilities operations for ZIP.
 */
public final class ZipUtils {
    private ZipUtils() {
    }

    public static InputStream streamOf(@NonNull ZipFile zip, @NonNull String name) throws IOException {
        val entry = zip.getEntry(name);
        if (entry == null) {
            throw new IOException(M.tr("err.zip.noEntry", name, zip.getName()));
        }
        return zip.getInputStream(entry);
    }

    public static String textOf(ZipFile zip, String name, String encoding) throws IOException {
        try (val in = streamOf(zip, name)) {
            return IOUtils.toString(in, encoding);
        }
    }

    public static void write(ZipOutputStream zipout, String name, String str, String encoding) throws IOException {
        zipout.putNextEntry(new ZipEntry(name));
        zipout.write(encoding == null ? str.getBytes() : str.getBytes(encoding));
        zipout.closeEntry();
    }

    public static void write(ZipOutputStream zipout, String name, Flob flob) throws IOException {
        zipout.putNextEntry(new ZipEntry(name));
        flob.writeTo(zipout);
        zipout.closeEntry();
    }

    /**
     * Writes text content in TextObject to zip archive.
     *
     * @param zipout   zip archive stream
     * @param name     name of entry to store text content
     * @param text     the Text
     * @param encoding encoding to encode text, if {@literal null} use platform encoding
     * @throws IOException if occurs IO errors when writing text
     */
    public static void write(ZipOutputStream zipout, String name, Text text, String encoding) throws IOException {
        zipout.putNextEntry(new ZipEntry(name));
        val writer = IOUtils.writerFor(zipout, encoding);
        text.writeTo(writer);
        writer.flush();
        zipout.closeEntry();
    }
}
