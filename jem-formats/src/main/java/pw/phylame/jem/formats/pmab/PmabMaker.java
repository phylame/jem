/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pw.phylame.jem.formats.pmab;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipOutputStream;

import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.epm.impl.ZipMaker;
import pw.phylame.jem.epm.util.MakerException;
import pw.phylame.jem.epm.util.ZipUtils;
import pw.phylame.jem.epm.util.xml.XmlRender;
import pw.phylame.jem.formats.util.M;
import pw.phylame.jem.util.Variants;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.jem.util.text.Text;
import pw.phylame.jem.util.text.Texts;
import pw.phylame.ycl.format.Converters;
import pw.phylame.ycl.io.PathUtils;
import pw.phylame.ycl.util.CollectUtils;
import pw.phylame.ycl.util.DateUtils;
import pw.phylame.ycl.util.MiscUtils;
import pw.phylame.ycl.util.StringUtils;

/**
 * PMAB e-book maker.
 */
public class PmabMaker extends ZipMaker<PmabOutConfig> {
    public PmabMaker() {
        super("pmab", PmabOutConfig.class);
    }

    @Override
    public void make(Book book, ZipOutputStream zipout, PmabOutConfig config) throws IOException, MakerException {
        if (config == null) {
            config = new PmabOutConfig();
        }
        int version = 3;
        if (StringUtils.isNotEmpty(config.version)) {
            char ch = config.version.charAt(0);
            if (ch == '3') {
                version = 3;
            } else if (ch == '2') {
                version = 2;
            } else {
                throw new MakerException(M.tr("pmab.make.unsupportedVersion", config.version));
            }
        }
        val tuple = new Tuple(zipout, config, version);
        writeMIME(tuple);
        writePBM(book, tuple);
        writePBC(book, tuple);
    }

    private void writeMIME(Tuple tuple) throws IOException {
        ZipUtils.write(tuple.zipout, PMAB.MIME_FILE, PMAB.MT_PMAB, "ASCII");
    }

    private void writePBM(Book book, Tuple tuple) throws IOException, MakerException {
        val writer = prepareXml("pbm", PMAB.PBM_XML_NS, tuple);
        switch (tuple.version) {
        case 3:
            writePBMHead("value", true, tuple);
            writePBMv3(book, tuple);
        break;
        case 2:
            writePBMHead("content", false, tuple);
            writePBMv2(book, tuple);
        break;
        }
        writeXml(tuple, writer, PMAB.PBM_FILE);
    }

    private void writePBC(Book book, Tuple tuple) throws IOException {
        val writer = prepareXml("pbc", PMAB.PBC_XML_NS, tuple);
        switch (tuple.version) {
        case 3:
            writePBCv3(book, tuple);
        break;
        case 2:
            writePBCv2(book, tuple);
        break;
        }
        writeXml(tuple, writer, PMAB.PBC_FILE);
    }

    private void writePBMHead(String tagName, boolean ignoreEmpty, Tuple tuple) throws IOException {
        val render = tuple.render;
        val meta = tuple.config.metadata;
        if (CollectUtils.isEmpty(meta)) {
            if (!ignoreEmpty) {
                render.startTag("head").endTag();
            }
            return;
        }
        render.startTag("head");
        for (val e : meta.entrySet()) {
            render.startTag("meta");
            render.attribute("name", e.getKey().toString());
            render.attribute(tagName, e.getValue().toString());
            render.endTag();
        }
        render.endTag();
    }

    private void writePBMv3(Book book, Tuple tuple) throws IOException {
        val render = tuple.render;
        writeV3Attributes(book, "", tuple);
        render.startTag("extensions");
        String key;
        for (val e : book.getExtensions().entries()) {
            key = e.getKey();
            if (!META_KEY.equals(key)) {
                writeV3Item(key, e.getValue(), "", tuple);
            }
        }
        render.endTag();
    }

    private void writeV3Attributes(Chapter chapter, String prefix, Tuple tuple) throws IOException {
        val render = tuple.render.startTag("attributes");
        for (val e : chapter.getAttributes().entries()) {
            writeV3Item(e.getKey(), e.getValue(), prefix, tuple);
        }
        render.endTag();
    }

    private void writeV3Item(String name, Object value, String prefix, Tuple tuple) throws IOException {
        val render = tuple.render.startTag("item").attribute("name", name);
        val config = tuple.config;
        String data;
        String type = Variants.typeOf(value);
        if (!type.equals(Variants.STRING)) {
            switch (type) {
            case Variants.TEXT:
                val dir = name.equals(Attributes.INTRO) ? config.textDir : config.extraDir;
                data = writeV3Text((Text) value, dir, prefix + name, tuple);
                type = null;
            break;
            case Variants.FLOB:
                data = writeFile((Flob) value, prefix + name, "type", tuple);
                type = null;
            break;
            case Variants.DATETIME:
                data = DateUtils.format((Date) value, config.dateFormat);
                type = type + ";format=" + config.dateFormat;
            break;
            case Variants.LOCALE:
                data = Converters.render((Locale) value, Locale.class);
            break;
            default:
                data = value.toString();
            break;
            }
        } else {
            data = value.toString();
        }
        if (type != null) {
            render.attribute("type", type);
        }
        render.text(data).endTag();
    }

    private String writeV3Text(Text text, String dir, String baseName, Tuple tuple) throws IOException {
        String[] objects = writeText(text, dir, baseName, tuple);
        tuple.render.attribute("type", "text/" + text.getType() + ";encoding=" + objects[1]);
        return objects[0];
    }

    private void writePBMv2(Book book, Tuple tuple) throws IOException, MakerException {
        val render = tuple.render.startTag("metadata").attribute("count",
                Integer.toString(book.getAttributes().size()));
        for (val e : book.getAttributes().entries()) {
            writePBMv2Attr(e.getKey(), e.getValue(), tuple);
        }
        render.endTag();

        render.startTag("extension").attribute("count", Integer.toString(book.getExtensions().size()));
        render.comment("The following data will be added to PMAB.");
        String key;
        for (val e : book.getExtensions().entries()) {
            key = e.getKey();
            if (!META_KEY.equals(key)) {
                writePBMv2Item(e.getKey(), e.getValue(), tuple);
            }
        }
        render.endTag();
    }

    private void writePBMv2Attr(String key, Object value, Tuple tuple) throws IOException, MakerException {
        val render = tuple.render.startTag("attr").attribute("name", key);
        String data;
        val type = Variants.typeOf(value);
        if (type.equals(Variants.TEXT)) {
            data = ((Text) value).getText();
        } else if (key.equals(Attributes.COVER)) {
            data = writeV2Cover((Flob) value, "", tuple);
        } else if (type.equals(Variants.DATETIME)) {
            data = DateUtils.format((Date) value, tuple.config.dateFormat);
        } else if (type.equals(Variants.LOCALE)) {
            data = Converters.render((Locale) value, Locale.class);
        } else {
            data = value.toString();
        }
        render.text(data).endTag();
    }

    private String writeV2Cover(Flob cover, String prefix, Tuple tuple) throws IOException {
        return writeFile(cover, prefix + "cover", "media-type", tuple);
    }

    private void writePBMv2Item(String key, Object value, Tuple tuple) throws IOException {
        val render = tuple.render.startTag("item").attribute("name", key);
        if (value instanceof Flob) {
            render.attribute("type", Variants.FLOB);
            render.startTag("object").attribute("href", writeFile((Flob) value, key, "media-type", tuple)).endTag();
        } else if (value instanceof Text) {
            render.attribute("type", Variants.FLOB);
            render.startTag("object");
            val text = (Text) value;
            render.attribute("media-type", "text/" + text.getType());
            render.attribute("href", writeV2Text(text, key, tuple));
            render.endTag();
        } else if (value instanceof Number) {
            render.attribute("type", "number").text(value.toString());
        } else {
            render.attribute("type", Variants.TEXT).text(value.toString());
        }
        render.endTag();
    }

    private String writeV2Text(Text text, String baseName, Tuple tuple) throws IOException {
        String[] objects = writeText(text, tuple.config.textDir, baseName, tuple);
        tuple.render.attribute("encoding", objects[1]);
        return objects[0];
    }

    private void writePBCv3(Book book, Tuple tuple) throws IOException {
        val render = tuple.render.startTag("toc");
        int count = 1;
        for (val sub : book) {
            writeV3Chapter(sub, Integer.toString(count), tuple);
            ++count;
        }
        render.endTag();
    }

    private void writeV3Chapter(Chapter chapter, String suffix, Tuple tuple) throws IOException {
        val render = tuple.render.startTag("chapter");
        val base = "chapter-" + suffix;

        // attributes
        writeV3Attributes(chapter, base + "-", tuple);

        // text
        val text = chapter.getText();
        if (text != null) {
            render.startTag("content").text(writeV3Text(text, tuple.config.textDir, base, tuple)).endTag();
        }

        int count = 1;
        for (val sub : chapter) {
            writeV3Chapter(sub, suffix + "-" + count, tuple);
            ++count;
        }
        render.endTag();
    }

    private void writePBCv2(Book book, Tuple tuple) throws IOException {
        val render = tuple.render
                .startTag("contents")
                .attribute("depth", Integer.toString(MiscUtils.depthOf((Chapter) book)));
        int count = 1;
        for (val sub : book) {
            writeV2Chapter(sub, Integer.toString(count), tuple);
            ++count;
        }
        render.endTag();
    }

    private void writeV2Chapter(Chapter chapter, String suffix, Tuple tuple) throws IOException {
        val render = tuple.render.startTag("chapter");
        val base = "chapter-" + suffix;

        // text
        val text = chapter.getText();
        if (text != null) {
            render.attribute("href", writeV2Text(text, base, tuple));
        }

        // size of sub chapters
        if (chapter.isSection()) {
            render.attribute("count", Integer.toString(chapter.size()));
        }

        // title
        render.startTag("title").text(Attributes.getTitle(chapter)).endTag();

        // cover
        val cover = Attributes.getCover(chapter);
        if (cover != null) {
            render.startTag("cover").attribute("href", writeV2Cover(cover, base + "-", tuple)).endTag();
        }
        // intro
        val intro = Attributes.getIntro(chapter);
        if (intro != null) {
            render.startTag("intro").attribute("href", writeV2Text(intro, base + "-intro", tuple)).endTag();
        }

        int count = 1;
        for (val sub : chapter) {
            writeV2Chapter(sub, suffix + "-" + count, tuple);
            ++count;
        }
        render.endTag();
    }

    // return href and encoding
    private String[] writeText(Text text, String dir, String baseName, Tuple tuple) throws IOException {
        val encoding = tuple.config.textEncoding != null ? tuple.config.textEncoding : PMAB.defaultEncoding;
        val type = text.getType();
        val href = dir + "/" + (type.equals(Texts.PLAIN) ? baseName + ".txt" : baseName + "." + type);
        ZipUtils.write(tuple.zipout, href, text, encoding);
        return new String[]{href, encoding};
    }

    private String writeFile(Flob file, String baseName, String mimeKey, Tuple tuple) throws IOException {
        val name = baseName + "." + PathUtils.extensionName(file.getName());
        val dir = file.getMime().startsWith("image/") ? tuple.config.imageDir : tuple.config.extraDir;
        return writeFile(file, dir, name, mimeKey, tuple);
    }

    private String writeFile(Flob file, String dir, String name, String mimeKey, Tuple tuple) throws IOException {
        val href = dir + "/" + name;
        ZipUtils.write(tuple.zipout, href, file);
        tuple.render.attribute(mimeKey, file.getMime());
        return href;
    }

    private StringWriter prepareXml(String root, String ns, Tuple tuple) throws IOException {
        StringWriter writer = new StringWriter();
        tuple.render.setOutput(writer)
                .startXml().docdecl(root)
                .startTag(root).attribute("version", tuple.config.version).attribute("xmlns", ns);
        return writer;
    }

    private void writeXml(Tuple tuple, StringWriter writer, String name) throws IOException {
        tuple.render.endTag().endXml();
        ZipUtils.write(tuple.zipout, name, writer.toString(), tuple.config.xmlConfig.encoding);
    }

    private class Tuple {
        private ZipOutputStream zipout;
        private PmabOutConfig config;
        private XmlRender render;
        private int version;

        private Tuple(ZipOutputStream zipout, PmabOutConfig config, int version) throws MakerException {
            this.zipout = zipout;
            this.version = version;
            this.config = config;
            render = new XmlRender(config.xmlConfig, false);
        }
    }
}
