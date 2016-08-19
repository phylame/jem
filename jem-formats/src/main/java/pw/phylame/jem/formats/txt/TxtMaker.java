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

package pw.phylame.jem.formats.txt;

import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.epm.base.AbstractMaker;
import pw.phylame.jem.epm.util.MakerException;
import pw.phylame.jem.epm.util.text.TextRender;
import pw.phylame.ycl.util.StringUtils;

import java.io.*;

/**
 * <tt>Maker</tt> implement for TXT book.
 */
public class TxtMaker extends AbstractMaker<TxtOutConfig> {
    public TxtMaker() {
        super("txt", TxtOutConfig.class);
    }

    @Override
    public void make(@NonNull Book book, @NonNull OutputStream output, TxtOutConfig config) throws IOException, MakerException {
        if (config == null) {
            config = new TxtOutConfig();
        }
        make(book, new BufferedWriter(new OutputStreamWriter(output, config.encoding)), config);
    }

    public void make(Book book, Writer writer, TxtOutConfig config) throws IOException {
        if (config == null) {
            config = new TxtOutConfig();
        }
        if (!(writer instanceof BufferedWriter)) {
            writer = new BufferedWriter(writer);
        }
        val lineSeparator = config.textConfig.lineSeparator;
        if (StringUtils.isNotEmpty(config.header)) {
            writer.append(config.header).append(lineSeparator);
        }
        writer.append(Attributes.getTitle(book)).append(lineSeparator);
        val author = Attributes.getAuthor(book);
        if (StringUtils.isNotEmpty(author)) {
            writer.append(author).append(lineSeparator);
        }
        val render = new TxtRender(writer, config.additionLine, lineSeparator);
        try {
            val intro = Attributes.getIntro(book);
            if (intro != null) {
                if (TextRender.renderText(intro, render, config.textConfig)) {
                    writer.write(lineSeparator);
                }
            }
            writer.write(lineSeparator);
            if (book.isSection()) {
                TextRender.renderBook(book, render, config.textConfig);
            } else {        // book has not sub-parts, then save its content
                TextRender.renderText(book.getText(), render, config.textConfig);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
        if (StringUtils.isNotEmpty(config.footer)) {
            writer.write(config.footer);
        }
        writer.flush();
    }
}
