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

package pw.phylame.jem.epm.base;

import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.epm.util.MakerException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

public abstract class ZipMaker<C extends ZipOutConfig> extends AbstractMaker<C> {
    protected ZipMaker(@NonNull String name, Class<C> clazz) {
        super(name, clazz);
    }

    public abstract void make(Book book, ZipOutputStream zipout, C config) throws IOException, MakerException;

    @Override
    public final void make(Book book, OutputStream output, C config) throws IOException, MakerException {
        try (val zipout = new ZipOutputStream(output)) {
            zipout.setMethod(config.zipMethod);
            zipout.setLevel(config.zipLevel);
            zipout.setComment(config.zipComment);
            make(book, zipout, config);
            zipout.flush();
        }
    }
}
