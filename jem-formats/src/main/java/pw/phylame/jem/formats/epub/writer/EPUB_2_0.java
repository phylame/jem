/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
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

import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.epm.util.MakerException;
import pw.phylame.jem.formats.epub.EPUB;
import pw.phylame.jem.formats.epub.Resource;
import pw.phylame.jem.formats.epub.ncx.NcxWriterFactory;
import pw.phylame.jem.formats.epub.opf.OpfWriterFactory;
import pw.phylame.jem.formats.util.M;
import pw.phylame.ycl.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;
import java.util.zip.ZipEntry;

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
        val config = tuple.config;
        val render = tuple.render;
        val zipout = tuple.zipout;
        if (StringUtils.isEmpty(config.uuid)) {
            config.uuid = getUUID(tuple.book);
        }
        // make and write NCX document
        val ncxWriter = NcxWriterFactory.getWriter("2005-1");
        if (ncxWriter == null) {
            throw new MakerException(M.tr("epub.make.v2.noNCX_2005_1"));
        }
        val writer = new StringWriter();
        render.setOutput(writer);
        ncxWriter.write(this, tuple);
        val ncxHref = EPUB.NCX_FILE;
        writeToOps(writer.toString(), ncxHref, config.xmlConfig.encoding);

        val resources = ncxWriter.getResources();
        resources.add(new Resource(EPUB.NCX_FILE_ID, ncxHref, EPUB.MT_NCX));

        val opfWriter = OpfWriterFactory.getWriter("2.0");
        if (opfWriter == null) {
            throw new MakerException(M.tr("epub.make.v2.noOPF_2_0"));
        }
        val opfPath = pathInOps(OPF_FILE);
        zipout.putNextEntry(new ZipEntry(opfPath));
        render.setOutput(zipout);
        opfWriter.write(ncxWriter.getCoverID(), resources, ncxWriter.getSpines(), EPUB.NCX_FILE_ID, ncxWriter.getGuides(), tuple);
        render.flush();
        zipout.closeEntry();

        writeContainer(opfPath);
    }

    private String getUUID(Book book) {
        // UUID of the book
        String uuid = book.getAttributes().get("uuid", null);
        if (StringUtils.isEmpty(uuid)) {
            uuid = Attributes.getISBN(book);
            if (StringUtils.isEmpty(uuid)) {
                uuid = UUID.randomUUID().toString();
            }
        }
        return uuid;
    }
}
