/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

package jem.formats.jar;

import jem.epm.util.text.TextWriter;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequiredArgsConstructor
class JarRender implements TextWriter {
    private final ZipOutputStream zipout;

    final List<JarMaker.NavItem> items = new LinkedList<>();

    // for generating entry name
    private int chapterCount = 1;
    private String name;
    private int length;
    private String title;

    @Override
    public void beginChapter(String title) throws IOException {
        name = String.valueOf(chapterCount++);
        zipout.putNextEntry(new ZipEntry(name));
        length = 0;
        this.title = title;
    }

    @Override
    public JarRender write(String text) throws IOException {
        val buf = text.getBytes(JAR.TEXT_ENCODING);
        zipout.write(buf);
        length += buf.length;
        return this;
    }

    @Override
    public void endChapter() throws IOException {
        zipout.closeEntry();
        items.add(new JarMaker.NavItem(name, length + 2, title));
    }
}
