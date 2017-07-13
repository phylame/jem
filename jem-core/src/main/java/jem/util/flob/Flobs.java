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

package jem.util.flob;

import jclp.io.IOUtils;
import jclp.io.RAFInputStream;
import jclp.util.Exceptions;
import jclp.vdm.VdmEntry;
import jclp.vdm.VdmReader;
import jem.util.Variants;
import lombok.Getter;
import lombok.NonNull;

import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.zip.ZipFile;

/**
 * Factory class for creating {@code Flob} instance.
 */
public final class Flobs {
    private Flobs() {
    }

    public static Flob forEmpty(String name, String mime) {
        return forBytes(name, null, mime);
    }

    public static Flob forFile(@NonNull File file, String mime) throws IOException {
        return new NormalFlob(file, mime);
    }

    public static Flob forZip(@NonNull ZipFile zipFile, @NonNull String entry, String mime) throws IOException {
        return new EntryFlob(zipFile, entry, mime);
    }

    public static Flob forVam(@NonNull VdmReader vam, @NonNull String entry, String mime) throws IOException {
        return new VamFlob(vam, entry, mime);
    }

    public static BlockFlob forBlock(@NonNull String name, @NonNull RandomAccessFile file, long offset, long size,
                                     String mime) throws IOException {
        return new BlockFlob(name, file, offset, size, mime);
    }

    public static Flob forURL(@NonNull URL url, String mime) {
        return new URLFlob(url, mime);
    }

    public static Flob forBytes(@NonNull String name, byte[] bytes, String mime) {
        return new ByteFlob(name, bytes, mime);
    }

    public static class NormalFlob extends AbstractFlob {
        static {
            Variants.mapClass(NormalFlob.class, Variants.FLOB);
        }

        @Getter
        private final File file;

        private NormalFlob(@NonNull File file, String mime) throws IOException {
            super(mime);
            if (!file.exists()) {
                throw Exceptions.forFileNotFound("No such file: %s", file);
            }
            if (file.isDirectory()) {
                throw Exceptions.forIllegalArgument("Expect file but specified directory: %s", file);
            }
            this.file = file;
        }

        @Override
        public String getName() {
            return file.getPath();
        }

        @Override
        public FileInputStream openStream() throws IOException {
            return new FileInputStream(file);
        }
    }

    private static class VamFlob extends AbstractFlob {

        static {
            Variants.mapClass(VamFlob.class, Variants.FLOB);
        }

        private final VdmReader vdmReader;
        private final VdmEntry vdmEntry;
        private final String name;

        VamFlob(VdmReader vdmReader, String name, String mime) throws IOException {
            super(mime);
            vdmEntry = vdmReader.entryFor(name);
            if (vdmEntry == null) {
                throw Exceptions.forIO("No such entry in vdm: %s", name);
            }
            this.vdmReader = vdmReader;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public InputStream openStream() throws IOException {
            return vdmReader.streamFor(vdmEntry);
        }

        @Override
        public String toString() {
            return vdmEntry.toString();
        }
    }

    private static class EntryFlob extends AbstractFlob {
        static {
            Variants.mapClass(EntryFlob.class, Variants.FLOB);
        }

        private final ZipFile zipFile;
        private final String name;

        private EntryFlob(@NonNull ZipFile zipFile, @NonNull String name, String mime) throws IOException {
            super(mime);
            if (zipFile.getEntry(name) == null) {
                throw Exceptions.forIO("No such entry in ZIP: %s", name);
            }
            this.zipFile = zipFile;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public InputStream openStream() throws IOException {
            return zipFile.getInputStream(zipFile.getEntry(name));
        }

        @Override
        public String toString() {
            return "zip://" + zipFile.getName() + '!' + super.toString();
        }
    }

    /**
     * File object that represents a block of source file.
     */
    public static class BlockFlob extends AbstractFlob {
        static {
            Variants.mapClass(BlockFlob.class, Variants.FLOB);
        }

        private final String name;
        private final RandomAccessFile file;

        public long offset, size;

        private BlockFlob(String name, RandomAccessFile file, long offset, long size, String mime) throws IOException {
            super(mime);
            if (size > (file.length() - offset)) {
                throw Exceptions.forIllegalArgument("offset(%d) + size(%d) > total(%d)", offset, size, file.length());
            }
            this.name = name;
            this.file = file;
            this.offset = offset;
            this.size = size;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public InputStream openStream() throws IOException {
            file.seek(offset);
            return new RAFInputStream(file, size);
        }

        @Override
        public byte[] readAll() throws IOException {
            file.seek(offset);
            byte[] buf = new byte[(int) size];
            int n = file.read(buf);
            if (n < size) {
                return Arrays.copyOf(buf, n);
            } else {
                return buf;
            }
        }

        @Override
        public long writeTo(OutputStream out) throws IOException {
            file.seek(offset);
            return IOUtils.copy(file, out, (int) size);
        }

        @Override
        public String toString() {
            return String.format("block://%s;offset=%d;size=%d", super.toString(), offset, size);
        }
    }

    public static class URLFlob extends AbstractFlob {
        static {
            Variants.mapClass(URLFlob.class, Variants.FLOB);
        }

        @Getter
        private final URL url;

        private URLFlob(URL url, String mime) {
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

    private static class ByteFlob extends AbstractFlob {
        static {
            Variants.mapClass(ByteFlob.class, Variants.FLOB);
        }

        private final String name;
        private final byte[] buf;

        private ByteFlob(String name, byte[] buf, String mime) {
            super(mime);
            this.name = name;
            this.buf = (buf != null) ? buf : new byte[0];
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public InputStream openStream() throws IOException {
            return new ByteArrayInputStream(buf);
        }

        @Override
        public byte[] readAll() throws IOException {
            return Arrays.copyOf(buf, buf.length);
        }

        @Override
        public long writeTo(OutputStream out) throws IOException {
            out.write(buf);
            return buf.length;
        }

        @Override
        public String toString() {
            return "bytes://" + super.toString();
        }
    }
}
