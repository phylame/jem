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

package jem.epm.impl;

import jclp.setting.Settings;
import jem.Book;
import jem.util.JemException;

import java.io.File;
import java.io.IOException;

public abstract class AbstractMaker implements FileMaker {
    @Override
    public void make(Book book, String output, Settings arguments) throws IOException, JemException {
        make(book, new File(output), arguments);
    }

    @SuppressWarnings("unchecked")
    protected <T> T get(Settings settings, String key) {
        return (T) settings.get("maker." + key);
    }
}
