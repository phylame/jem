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

package jem.epm.impl;

import jem.epm.util.config.AbstractConfig;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

public abstract class ZipParser<C extends ZipParser.ZipInConfig> extends AbstractParser<ZipFile, C> {
    protected ZipParser(String name, Class<C> clazz) {
        super(name, clazz);
    }

    @Override
    protected ZipFile openInput(File file, C config) throws IOException {
        return new ZipFile(file);
    }

    public static class ZipInConfig extends AbstractConfig {
    }
}