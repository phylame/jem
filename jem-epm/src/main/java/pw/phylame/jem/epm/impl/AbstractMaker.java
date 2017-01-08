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

package pw.phylame.jem.epm.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.epm.Maker;
import pw.phylame.jem.epm.util.MakerException;
import pw.phylame.jem.epm.util.config.EpmConfig;
import pw.phylame.jem.util.JemException;

public abstract class AbstractMaker<C extends EpmConfig> extends EpmBase<C> implements Maker {

    protected AbstractMaker(String name, Class<C> clazz) {
        super(name, clazz);
    }

    public abstract void make(Book book, OutputStream output, C config) throws IOException, MakerException;

    @Override
    public final void make(@NonNull Book book, @NonNull File file, Map<String, Object> args) throws IOException, JemException {
        try (val out = new BufferedOutputStream(new FileOutputStream(file))) {
            make(book, out, fetchConfig(args));
        }
    }
}
