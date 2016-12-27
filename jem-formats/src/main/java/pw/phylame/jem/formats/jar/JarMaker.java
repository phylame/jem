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

package pw.phylame.jem.formats.jar;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.epm.impl.ZipMaker;
import pw.phylame.jem.epm.util.MakerException;
import pw.phylame.jem.epm.util.ZipUtils;
import pw.phylame.jem.epm.util.text.TextRender;
import pw.phylame.jem.formats.util.M;
import pw.phylame.jem.util.Build;
import pw.phylame.ycl.io.IOUtils;

/**
 * <tt>Maker</tt> implement for JAR book.
 */
public class JarMaker extends ZipMaker<JarOutConfig> {
    public JarMaker() {
        super("jar", JarOutConfig.class);
    }

    @Override
    public void make(Book book, ZipOutputStream zipout, JarOutConfig config) throws IOException, MakerException {
        if (config == null) {
            config = new JarOutConfig();
        }
        // JAR template
        copyTemplate(zipout, config.jarTemplate);

        val tuple = new Tuple(book, config, zipout);

        // MANIFEST
        val title = Attributes.getTitle(book);
        val manifest = String.format(JAR.MANIFEST_TEMPLATE, "Jem", Build.VERSION, title, config.vendor, title);
        ZipUtils.write(zipout, JAR.MANIFEST_FILE, manifest, JAR.METADATA_ENCODING);

        val render = new JarRender(zipout);
        try {
            TextRender.renderBook(book, render, config.textConfig);
        } catch (Exception e) {
            throw new IOException(e);
        }

        // navigation
        try {
            writeMeta(render.items, tuple);
        } catch (Exception e) {
            throw new MakerException(e);
        }
    }

    private void copyTemplate(ZipOutputStream zipout, String path) throws IOException, MakerException {
        val in = IOUtils.openResource(path, JarMaker.class.getClassLoader());
        if (in == null) {
            throw new MakerException(M.tr("jar.make.noTemplate", path));
        }
        try (val zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                zipout.putNextEntry(new ZipEntry(entry.getName()));
                IOUtils.copy(zip, zipout, -1);
                zip.closeEntry();
                zipout.closeEntry();
            }
        } finally {
            in.close();
        }
    }

    private void writeMeta(List<NavItem> items, Tuple tuple) throws Exception {
        val zipout = tuple.zipout;
        zipout.putNextEntry(new ZipEntry("0"));
        val output = new DataOutputStream(zipout);
        output.writeInt(JAR.MAGIC_NUMBER);
        val title = Attributes.getTitle(tuple.book);
        byte[] data = title.getBytes(JAR.METADATA_ENCODING);
        output.writeByte(data.length);
        output.write(data);

        data = String.valueOf(items.size()).getBytes(JAR.METADATA_ENCODING);
        output.writeShort(data.length);
        output.write(data);

        for (NavItem item : items) {
            val str = item.name + "," + item.size + "," + item.title;
            data = str.getBytes(JAR.METADATA_ENCODING);
            output.writeShort(data.length);
            output.write(data);
        }
        output.writeShort(0);  // what?
        val intro = Attributes.getIntro(tuple.book);
        val str = intro != null ? TextRender.renderText(intro, tuple.config.textConfig) : "";
        data = str.getBytes(JAR.METADATA_ENCODING);
        output.writeShort(data.length);
        output.write(data);
        zipout.closeEntry();
    }

    private class Tuple {
        private Book book;
        private JarOutConfig config;
        private ZipOutputStream zipout;

        private Tuple(Book book, JarOutConfig config, ZipOutputStream zipout) {
            this.book = book;
            this.config = config;
            this.zipout = zipout;
        }
    }

    static class NavItem {
        final String name;
        final int size;
        final String title;

        NavItem(String name, int size, String title) {
            this.name = name;
            this.size = size;
            this.title = title;
        }
    }
}
