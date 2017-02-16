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

package jem.epm.impl;

import jem.Book;
import jem.epm.util.MakerException;
import jem.epm.util.config.AbstractConfig;
import jem.epm.util.config.Configured;
import jem.util.Build;
import lombok.NonNull;
import lombok.val;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.ZipOutputStream;

public abstract class ZipMaker<C extends ZipMaker.ZipOutConfig> extends AbstractMaker<C> {

    protected ZipMaker(String name, Class<C> clazz) {
        super(name, clazz);
    }

    public abstract void make(Book book, ZipOutputStream zipout, C config) throws IOException, MakerException;

    @Override
    public final void make(@NonNull Book book, @NonNull OutputStream output, C config) throws IOException, MakerException {
        try (val zipout = new ZipOutputStream(output)) {
            zipout.setMethod(config.zipMethod);
            zipout.setLevel(config.zipLevel);
            zipout.setComment(config.zipComment);
            make(book, zipout, config);
            zipout.flush();
        }
    }

    public static class ZipOutConfig extends AbstractConfig {
        public static final String ZIP_METHOD = "zip.method";
        public static final String ZIP_LEVEL = "zip.level";
        public static final String ZIP_COMMENT = "zip.comment";

        /**
         * Compression method of ZIP entry.
         */
        @Configured(ZIP_METHOD)
        public int zipMethod = Deflater.DEFLATED;

        /**
         * Compression level of ZIP entry.
         */
        @Configured(ZIP_LEVEL)
        public int zipLevel = Deflater.DEFAULT_COMPRESSION;

        /**
         * Comment string for ZIP.
         */
        @Configured(ZIP_COMMENT)
        public String zipComment = String.format("generated by %s v%s", Build.NAME, Build.VERSION);
    }
}
