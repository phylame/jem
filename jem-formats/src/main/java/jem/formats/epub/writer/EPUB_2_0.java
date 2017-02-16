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

package jem.formats.epub.writer;

import static pw.phylame.commons.util.StringUtils.isEmpty;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;
import java.util.zip.ZipEntry;

import jem.Attributes;
import jem.Book;
import jem.epm.util.MakerException;
import jem.formats.epub.EPUB;
import jem.formats.epub.item.Resource;
import jem.formats.epub.ncx.NcxFactory;
import jem.formats.epub.opf.OpfFactory;
import jem.formats.util.M;
import lombok.val;

/**
 * ePub 2.0 implements.
 * <p>
 * Using components:
 * <ul>
 * <li>OPF: 2.0</li>
 * <li>NCX: 2005-1</li>
 * </ul>
 */
class EPUB_2_0 extends EpubWriter {

    @Override
    protected void write() throws IOException, MakerException {
        val config = data.config;
        val render = data.render;
        val zipout = data.zipout;
        if (isEmpty(config.uuid)) {
            detectUUID(data.book);
        }
        // make and write NCX document
        val ncx = NcxFactory.getWriter("2005-1");
        if (ncx == null) {
            throw new MakerException(M.tr("epub.make.v2.noNCX_2005_1"));
        }
        val writer = new StringWriter();
        render.setOutput(writer);
        ncx.write(this, data);

        val ncxHref = EPUB.NCX_FILE;
        writeToOps(writer.toString(), ncxHref, config.xmlConfig.encoding);

        val resources = ncx.getResources();
        resources.add(new Resource(EPUB.NCX_FILE_ID, ncxHref, EPUB.MT_NCX));

        val opf = OpfFactory.getWriter("2.0");
        if (opf == null) {
            throw new MakerException(M.tr("epub.make.v2.noOPF_2_0"));
        }
        val opfPath = pathInOps(EPUB.OPF_FILE);
        zipout.putNextEntry(new ZipEntry(opfPath));
        render.setOutput(zipout);
        opf.write(ncx.getCoverId(), resources, ncx.getSpines(), EPUB.NCX_FILE_ID, ncx.getGuides(), data);
        render.flush();
        zipout.closeEntry();

        writeContainer(opfPath);
    }

    private void detectUUID(Book book) {
        val config = data.config;
        config.uuid = book.getAttributes().get("uuid", null);
        if (isEmpty(config.uuid)) {
            config.uuid = Attributes.getISBN(book);
            config.uuidType = "isbn";
            if (isEmpty(config.uuid)) {
                config.uuid = UUID.randomUUID().toString();
                config.uuidType = "uuid";
            }
        }
    }
}
