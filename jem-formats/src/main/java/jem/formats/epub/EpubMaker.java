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

package jem.formats.epub;

import java.io.IOException;
import java.util.zip.ZipOutputStream;

import jem.core.Book;
import jem.epm.impl.ZipMaker;
import jem.epm.util.MakerException;
import jem.epm.util.ZipUtils;
import jem.formats.epub.writer.EpubWriterFactory;
import jem.formats.util.M;
import lombok.NonNull;
import lombok.val;

/**
 * ePub e-book maker.
 */
public class EpubMaker extends ZipMaker<EpubOutConfig> {
    public EpubMaker() {
        super("epub", EpubOutConfig.class);
    }

    @Override
    public void make(@NonNull Book book, @NonNull ZipOutputStream zipout, EpubOutConfig config)
            throws IOException, MakerException {
        if (config == null) {
            config = new EpubOutConfig();
        }
        val writer = EpubWriterFactory.getWriter(config.version);
        if (writer == null) {
            throw new MakerException(M.tr("epub.make.unsupportedVersion", config.version));
        }
        ZipUtils.write(zipout, EPUB.MIME_FILE, EPUB.MT_EPUB, "ASCII");
        writer.write(book, config, zipout);
    }
}
