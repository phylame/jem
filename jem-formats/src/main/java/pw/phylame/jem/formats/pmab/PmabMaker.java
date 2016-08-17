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

package pw.phylame.jem.formats.pmab;

import lombok.val;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.core.Jem;
import pw.phylame.jem.epm.base.ZipMaker;
import pw.phylame.jem.epm.util.MakerException;
import pw.phylame.jem.epm.util.Messages;
import pw.phylame.jem.epm.util.ZipUtils;
import pw.phylame.jem.epm.util.xml.XmlRender;
import pw.phylame.jem.util.Variants;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.jem.util.text.Text;
import pw.phylame.ycl.util.MiscUtils;
import pw.phylame.ycl.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipOutputStream;

/**
 * PMAB e-book maker.
 */
public class PmabMaker extends ZipMaker<PmabOutConfig> {
    private PmabOutConfig config;

    private int version;

    public PmabMaker() {
        super("pmab", PmabOutConfig.class);
    }

    @Override
    public void make(Book book, ZipOutputStream zipout, PmabOutConfig config) throws IOException, MakerException {
        this.config = config == null ? new PmabOutConfig() : config;
        if (StringUtils.isNotEmpty(this.config.version)) {
            char ch = this.config.version.charAt(0);
            if (ch == '3') {
                version = 3;
            } else if (ch == '2') {
                version = 2;
            } else {
                throw new MakerException(Messages.tr("pmab.make.unsupportedVersion", this.config.version))
            }
        }
        writeMIME(zipout);
        val render = new XmlRender(this.config.xmlConfig, false);
        writePBM(book, zipout, render);
        writePBC(book, zipout, render);
        this.config = null;
    }

    private void writeMIME(ZipOutputStream zipout) throws IOException {
        ZipUtils.writeString(zipout, PMAB.MIME_FILE, PMAB.MT_PMAB, "ASCII");
    }

    private void writePBM(Book book, ZipOutputStream zipout, XmlRender xmlRender) throws IOException, MakerException {
        StringWriter writer = prepareXml(xmlRender, "pbm", config.version, PMAB.PBM_XML_NS);
        switch (version) {
            case 3:
                writePBMHead("value", true, xmlRender);
                writePBMv3(book, zipout, xmlRender);
                break;
            case 2:
                writePBMHead("content", false, xmlRender);
                writePBMv2(book, zipout, xmlRender);
                break;
        }
        writeXml(xmlRender, writer, PMAB.PBM_FILE, zipout);
    }

    private void writePBC(Book book, ZipOutputStream zipout, XmlRender xmlRender) throws IOException {
        StringWriter writer = prepareXml(xmlRender, "pbc", config.version, PMAB.PBC_XML_NS);
        switch (version) {
            case 3:
                writePBCv3(book, zipout, xmlRender);
                break;
            case 2:
                writePBCv2(book, zipout, xmlRender);
                break;
        }
        writeXml(xmlRender, writer, PMAB.PBC_FILE, zipout);
    }

    private void writePBMHead(String valueName, boolean ignoreEmpty, XmlRender xmlRender) throws IOException {
        Map<Object, Object> metaInfo = config.metaInfo;
        if (MiscUtils.isEmpty(metaInfo)) {
            if (!ignoreEmpty) {
                xmlRender.startTag("head").endTag();
            }
            return;
        }
        xmlRender.startTag("head");
        for (Map.Entry<Object, Object> entry : metaInfo.entrySet()) {
            xmlRender.startTag("meta").attribute("name", entry.getKey().toString());
            xmlRender.attribute(valueName, entry.getValue().toString()).endTag();
        }
        xmlRender.endTag();
    }

    private void writePBMv3(Book book, ZipOutputStream zipout, XmlRender xmlRender) throws IOException {
        writeV3Attributes(book, "", zipout, xmlRender);
        xmlRender.startTag("extensions");
        for (val e : book.getExtensions().entries()) {
            val key = e.getKey();
//            if (key.equals(FileInfo.FILE_INFO)) {
//                continue;
//            }
            writeV3Item(key, e.getValue(), "", zipout, xmlRender);
        }
        xmlRender.endTag();
    }

    private void writeV3Attributes(Chapter chapter, String prefix, ZipOutputStream zipout,
                                   XmlRender xmlRender) throws IOException {
        xmlRender.startTag("attributes");
        for (Map.Entry<String, Object> entry : chapter.attributeEntries()) {
            writeV3Item(entry.getKey(), entry.getValue(), prefix, zipout, xmlRender);
        }
        xmlRender.endTag();
    }

    private void writeV3Item(CharSequence key, Object value, String prefix, ZipOutputStream zipout,
                             XmlRender xmlRender) throws IOException {
        xmlRender.startTag("item").attribute("name", key);
        String text;
        String type = Jem.typeOfVariant(value);
        if (!type.equals(Jem.STRING)) {
            switch (type) {
                case Variants.TEXT:
                    String dir;
                    if (key.equals(Chapter.INTRO)) {    // only intro stored to text dir
                        dir = config.textDir;
                    } else {
                        dir = config.extraDir;
                    }
                    text = writeV3Text((Text) value, dir, prefix + key, zipout, xmlRender);
                    type = null;
                    break;
                case Variants.FLOB:
                    text = writeFile((Flob) value, prefix + key, "type", zipout, xmlRender);
                    type = null;
                    break;
                case Variants.DATETIME:
                    text = formatDate((Date) value, config.dateFormat);
                    type = type + ";format=" + config.dateFormat;
                    break;
                case Variants.LOCALE:
                    text = formatLocale((Locale) value);
                    break;
                default:
                    text = value.toString();
                    break;
            }
        } else {
            text = (String) value;
        }
        if (type != null) {
            xmlRender.attribute("type", type);
        }
        xmlRender.text(text).endTag();
    }

    private String writeV3Text(Text text, String dir, String baseName, ZipOutputStream zipout,
                               XmlRender xmlRender) throws IOException {
        String[] objects = writeText(text, dir, baseName, zipout);
        xmlRender.attribute("type", "text/" + text.getType() + ";encoding=" + objects[1]);
        return objects[0];
    }

    private void writePBMv2(Book book, ZipOutputStream zipout, XmlRender xmlRender) throws IOException,
            MakerException {
        xmlRender.startTag("metadata");
        xmlRender.attribute("count", Integer.toString(book.attributeCount()));
        for (Map.Entry<String, Object> entry : book.attributeEntries()) {
            writePBMv2Attr(entry.getKey(), entry.getValue(), zipout, xmlRender);
        }
        xmlRender.endTag();

        xmlRender.startTag("extension");
        int size = book.extensionCount();
        if (book.hasExtension(FileInfo.FILE_INFO)) {
            --size;
        }
        xmlRender.attribute("count", Integer.toString(size));
        xmlRender.comment("The following data will be added to PMAB.");
        for (Map.Entry<String, Object> entry : book.extensionEntries()) {
            String key = entry.getKey();
            if (key.equals(FileInfo.FILE_INFO)) {
                continue;
            }
            writePBMv2Item(xmlRender, key, entry.getValue(), zipout);
        }
        xmlRender.endTag();
    }

    private void writePBMv2Attr(String key, Object value, ZipOutputStream zipout, XmlRender xmlRender)
            throws IOException, MakerException {
        xmlRender.startTag("attr").attribute("name", key);
        String text;
        String type = Jem.typeOfVariant(value);
        if (type.equals(Jem.TEXT)) {
            text = fetchText((Text) value, "");
        } else if (key.equals(Chapter.COVER)) {
            text = writeV2Cover((Flob) value, "", zipout, xmlRender);
        } else if (type.equals(Jem.DATETIME)) {
            text = formatDate((Date) value, config.dateFormat);
        } else if (type.equals(Jem.LOCALE)) {
            text = formatLocale((Locale) value);
        } else {
            text = value.toString();
        }
        xmlRender.text(text).endTag();
    }

    private String writeV2Cover(Flob cover, String prefix, ZipOutputStream zipout,
                                XmlRender xmlRender) throws IOException {
        String name = prefix + "cover." + IOUtils.getExtension(cover.getName());
        return writeFile(cover, config.imageDir, name, "media-type", zipout, xmlRender);
    }

    private void writePBMv2Item(XmlRender xmlRender, String key, Object value, ZipOutputStream zipout)
            throws IOException {
        xmlRender.startTag("item").attribute("name", key);
        String text = null;
        if (value instanceof Flob) {
            xmlRender.attribute("type", "file");
            xmlRender.startTag("object");
            String href = writeFile((Flob) value, key, "media-type", zipout, xmlRender);
            xmlRender.attribute("href", href).endTag();
        } else if (value instanceof Text) {
            xmlRender.attribute("type", "file");
            xmlRender.startTag("object");
            Text tb = (Text) value;
            String href = writeV2Text(tb, config.extraDir, key, zipout, xmlRender);
            xmlRender.attribute("media-type", "text/" + tb.getType());
            xmlRender.attribute("href", href).endTag();
        } else if (value instanceof Number) {
            xmlRender.attribute("type", "number");
            text = value.toString();
        } else {
            xmlRender.attribute("type", "text");
            text = value.toString();
        }
        if (text != null) {
            xmlRender.text(text);
        }
        xmlRender.endTag();
    }

    private String writeV2Text(Text text, String dir, String baseName, ZipOutputStream zipout,
                               XmlRender xmlRender) throws IOException {
        String[] objects = writeText(text, dir, baseName, zipout);
        xmlRender.attribute("encoding", objects[1]);
        return objects[0];
    }

    private void writePBCv3(Book book, ZipOutputStream zipout, XmlRender xmlRender) throws IOException {
        xmlRender.startTag("toc");
        int count = 1;
        for (Chapter chapter : book) {
            writeV3Chapter(chapter, Integer.toString(count), zipout, xmlRender);
            ++count;
        }
        xmlRender.endTag();
    }

    private void writeV3Chapter(Chapter chapter, String suffix, ZipOutputStream zipout, XmlRender xmlRender)
            throws IOException {
        xmlRender.startTag("chapter");
        String base = "chapter-" + suffix;

        // attributes
        writeV3Attributes(chapter, base + "-", zipout, xmlRender);

        // content
        Text content = chapter.getContent();
        if (content != null) {
            xmlRender.startTag("content");
            String href = writeV3Text(content, config.textDir, base, zipout, xmlRender);
            xmlRender.text(href).endTag();
        }

        int count = 1;
        for (Chapter sub : chapter) {
            writeV3Chapter(sub, suffix + "-" + count, zipout, xmlRender);
            ++count;
        }
        xmlRender.endTag();
    }

    private void writePBCv2(Book book, ZipOutputStream zipout, XmlRender xmlRender) throws IOException {
        xmlRender.startTag("contents");
        xmlRender.attribute("depth", Integer.toString(Jem.depthOf(book)));
        int count = 1;
        for (Chapter chapter : book) {
            writeV2Chapter(chapter, Integer.toString(count), zipout, xmlRender);
            ++count;
        }
        xmlRender.endTag();
    }

    private void writeV2Chapter(Chapter chapter, String suffix, ZipOutputStream zipout, XmlRender xmlRender)
            throws IOException {
        xmlRender.startTag("chapter");
        String base = "chapter-" + suffix;

        // content
        Text content = chapter.getContent();
        String href = writeV2Text(content, config.textDir, base, zipout, xmlRender);
        xmlRender.attribute("href", href);

        // size of sub chapters
        int size = chapter.size();
        if (size != 0) {
            xmlRender.attribute("count", Integer.toString(size));
        }

        // title
        xmlRender.startTag("title").text(chapter.getTitle()).endTag();

        // cover
        Flob cover = chapter.getCover();
        if (cover != null) {
            xmlRender.startTag("cover");
            href = writeV2Cover(cover, base + "-", zipout, xmlRender);
            xmlRender.attribute("href", href).endTag();
        }
        // intro
        Text intro = chapter.getIntro();
        if (intro != null) {
            xmlRender.startTag("intro");
            href = writeV2Text(intro, config.textDir, base + "-intro", zipout, xmlRender);
            xmlRender.attribute("href", href).endTag();
        }

        int count = 1;
        for (Chapter sub : chapter) {
            writeV2Chapter(sub, suffix + "-" + count, zipout, xmlRender);
            ++count;
        }
        xmlRender.endTag();
    }

    // return href and encoding
    private String[] writeText(Text text, String dir, String baseName, ZipOutputStream zipout)
            throws IOException {
        String encoding = config.textEncoding != null ? config.textEncoding : PMAB.defaultEncoding;
        String href;
        String type = text.getType();
        if (type.equals(Text.PLAIN)) {
            href = baseName + ".txt";
        } else {
            href = baseName + "." + type;
        }
        href = dir + "/" + href;
        ZipUtils.writeText(text, href, encoding, zipout);
        return new String[]{href, encoding};
    }

    private String writeFile(Flob file, String baseName, String mimeKey, ZipOutputStream zipout,
                             XmlRender xmlRender) throws IOException {
        String name = baseName + "." + IOUtils.getExtension(file.getName());
        String dir;
        if (file.getMime().startsWith("image/")) {  // image file stored to image dir
            dir = config.imageDir;
        } else {
            dir = config.extraDir;
        }
        return writeFile(file, dir, name, mimeKey, zipout, xmlRender);
    }

    private String writeFile(Flob file, String dir, String name, String mimeKey, ZipOutputStream zipout,
                             XmlRender xmlRender) throws IOException {
        String href = dir + "/" + name;
        ZipUtils.writeFile(file, href, zipout);
        xmlRender.attribute(mimeKey, file.getMime());
        return href;
    }

    private StringWriter prepareXml(XmlRender xmlRender, String root, String version, String ns)
            throws IOException {
        StringWriter writer = new StringWriter();
        xmlRender.setOutput(writer);
        xmlRender.startXml();
        xmlRender.docdecl(root);
        xmlRender.startTag(root).attribute("version", version);
        xmlRender.attribute("xmlns", ns);
        return writer;
    }

    private void writeXml(XmlRender xmlRender, StringWriter writer, String name, ZipOutputStream zipout)
            throws IOException {
        xmlRender.endTag();
        xmlRender.endXml();
        ZipUtils.writeString(writer.toString(), name, config.xmlConfig.encoding, zipout);
    }
}
