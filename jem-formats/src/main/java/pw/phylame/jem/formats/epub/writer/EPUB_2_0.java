/*
 * Copyright 2014-2015 Peng Wan <phylame@163.com>
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

package pw.phylame.jem.formats.epub.writer;

import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.io.IOException;
import java.io.StringWriter;

import pw.phylame.jem.core.Book;
import pw.phylame.jem.formats.epub.Resource;
import pw.phylame.jem.formats.util.ExceptionFactory;
import pw.phylame.jem.formats.util.MakerException;
import pw.phylame.jem.formats.util.text.TextUtils;

import pw.phylame.jem.formats.epub.EPUB;
import pw.phylame.jem.formats.epub.ncx.NcxWriter;
import pw.phylame.jem.formats.epub.ncx.NcxWriterFactory;
import pw.phylame.jem.formats.epub.opf.OpfWriter;
import pw.phylame.jem.formats.epub.opf.OpfWriterFactory;

/**
 * ePub 2.0 implements.
 * Using components:
 * <ul>
 * <li>OPF: 2.0</li>
 * <li>NCX: 2005-1</li>
 * </ul>
 */
class EPUB_2_0 extends EpubWriter {
    public static final String OPF_FILE = "content.opf";

    @Override
    protected void write() throws IOException, MakerException {
        if (TextUtils.isEmpty(config.uuid)) {
            config.uuid = getUUID(book);
        }
        // make and write NCX document
        NcxWriter ncxWriter = NcxWriterFactory.getWriter("2005-1");
        if (ncxWriter == null) {
            throw ExceptionFactory.makerException("epub.make.v2.noNCX_2005_1");
        }
        StringWriter writer = new StringWriter();
        xmlRender.setOutput(writer);
        ncxWriter.write(book, config, xmlRender, this, zipout);
        String ncxHref = EPUB.NCX_FILE;
        writeIntoOps(writer.toString(), ncxHref, config.xmlConfig.encoding);

        List<Resource> resources = ncxWriter.getResources();
        resources.add(new Resource(EPUB.NCX_FILE_ID, ncxHref, EPUB.MT_NCX));

        OpfWriter opfWriter = OpfWriterFactory.getWriter("2.0");
        if (opfWriter == null) {
            throw ExceptionFactory.makerException("epub.make.v2.noOPF_2_0");
        }
        String opfPath = pathInOps(OPF_FILE);
        zipout.putNextEntry(new ZipEntry(opfPath));
        xmlRender.setOutput(zipout);
        opfWriter.write(book, config, xmlRender, ncxWriter.getCoverID(),
                resources, ncxWriter.getSpineItems(), EPUB.NCX_FILE_ID,
                ncxWriter.getGuideItems());
        xmlRender.flush();
        zipout.closeEntry();

        writeContainer(opfPath);
    }

    private String getUUID(Book book) {
        // UUID of the book
        String uuid = book.stringAttribute("uuid", null);
        if (TextUtils.isEmpty(uuid)) {
            uuid = book.stringAttribute("isbn", null);
            if (TextUtils.isEmpty(uuid)) {
                uuid = UUID.randomUUID().toString();
            }
        }
        return uuid;
    }
}
