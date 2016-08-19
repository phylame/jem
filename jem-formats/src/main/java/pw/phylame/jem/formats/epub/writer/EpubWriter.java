/*
 * Copyright 2016 Peng Wan <phylame@163.com>
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

import pw.phylame.jem.core.Book;
import pw.phylame.jem.epm.util.MakerException;
import pw.phylame.jem.epm.util.ZipUtils;
import pw.phylame.jem.epm.util.xml.XmlRender;
import pw.phylame.jem.formats.epub.EPUB;
import pw.phylame.jem.formats.epub.EpubOutConfig;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.jem.util.text.Text;

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

    protected Book book;
    protected EpubOutConfig config;
    protected ZipOutputStream zipout;
    protected XmlRender xmlRender;

    public void write(Book book, EpubOutConfig config, ZipOutputStream zipout) throws IOException,
            MakerException {
        this.book = book;
        this.config = config;
        this.zipout = zipout;
        xmlRender = new XmlRender(config.xmlConfig, false);
        write();
    }

    protected abstract void write() throws IOException, MakerException;

    public String pathInOps(String name) {
        return OPS_DIR + "/" + name;
    }

    public void writeIntoOps(Flob file, String name) throws IOException {
        ZipUtils.writeFile(zipout, pathInOps(name), file);
    }

    public void writeIntoOps(String text, String name, String encoding) throws IOException {
        ZipUtils.writeString(zipout, pathInOps(name), text, encoding);
    }

    public void writeIntoOps(Text text, String name, String encoding) throws IOException {
        ZipUtils.writeText(zipout, pathInOps(name), text, encoding);
    }

    protected void writeContainer(String opfPath) throws IOException {
        StringWriter writer = new StringWriter();
        xmlRender.setOutput(writer);
        xmlRender.startXml();
        xmlRender.startTag("container").attribute("version", CONTAINER_VERSION);
        xmlRender.attribute("xmlns", CONTAINER_XML_NS);

        xmlRender.startTag("rootfiles");
        xmlRender.startTag("rootfile").attribute("full-path", opfPath);
        xmlRender.attribute("media-type", EPUB.MT_OPF).endTag();
        xmlRender.endTag();

        xmlRender.endTag();
        xmlRender.endXml();

        ZipUtils.writeString(zipout, EPUB.CONTAINER_FILE, writer.toString(), config.xmlConfig.encoding);
    }
}
