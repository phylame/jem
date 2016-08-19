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

package pw.phylame.jem.formats.umd;

import pw.phylame.jem.epm.util.text.TextWriter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.List;

class UmdRender implements TextWriter {
    private final UmdMaker maker;
    private final RandomAccessFile file;
    final List<Long> offsets;
    final List<String> titles;

    UmdRender(UmdMaker maker, RandomAccessFile file) {
        this.maker = maker;
        this.file = file;
        this.offsets = new LinkedList<>();
        this.titles = new LinkedList<>();
    }

    @Override
    public void startChapter(String title) throws Exception {
        offsets.add(file.getFilePointer());
        titles.add(title);
    }

    @Override
    public void writeText(String text) throws IOException {
        maker.writeString(file, text);
    }

    @Override
    public void endChapter() throws Exception {
    }
}