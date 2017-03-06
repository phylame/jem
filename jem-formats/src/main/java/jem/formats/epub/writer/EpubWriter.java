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

import jem.Book;
import jem.epm.util.MakerException;
import jem.epm.util.ZipUtils;
import jem.epm.util.xml.XmlRender;
import jem.formats.epub.EPUB;
import jem.formats.epub.EpubOutConfig;
import jem.formats.epub.OutData;
import jem.util.flob.Flob;
import jem.util.text.Text;
import lombok.val;

import java.io.IOException;
import java.io.StringWriter;
import java.util.zip.ZipOutputStream;

/**
 * Common ePub writer.
 */
public abstract class EpubWriter {
    public static final String CONTAINER_XML_NS = "urn:oasis:names:tc:opendocument:xmlns:container";
    public static final String CONTAINER_VERSION = "1.0";
    public static final String OPS_DIR = "OEBPS";

    protected OutData data;

    public void write(Book book, EpubOutConfig config, ZipOutputStream zipout) throws IOException, MakerException {
        data = new OutData(book, new XmlRender(config.xmlConfig, false), config, zipout);
        write();
    }

    protected abstract void write() throws IOException, MakerException;

    public String pathInOps(String name) {
        return OPS_DIR + "/" + name;
    }

    public void writeToOps(Flob file, String name) throws IOException {
        ZipUtils.write(data.zipout, pathInOps(name), file);
    }

    public void writeToOps(String text, String name, String encoding) throws IOException {
        ZipUtils.write(data.zipout, pathInOps(name), text, encoding);
    }

    public void writeToOps(Text text, String name, String encoding) throws IOException {
        ZipUtils.write(data.zipout, pathInOps(name), text, encoding);
    }

    protected void writeContainer(String opfPath) throws IOException {
        val render = data.render;
        val writer = new StringWriter();
        render.setOutput(writer);
        render.beginXml();
        render.beginTag("container").attribute("version", CONTAINER_VERSION);
        render.attribute("xmlns", CONTAINER_XML_NS);

        render.beginTag("rootfiles");
        render.beginTag("rootfile").attribute("full-path", opfPath).attribute("media-type", EPUB.MT_OPF).endTag();
        render.endTag();

        render.endTag();
        render.endXml();

        ZipUtils.write(data.zipout, EPUB.CONTAINER_FILE, writer.toString(), data.config.xmlConfig.encoding);
    }
}
