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

import jem.epm.util.E;
import jem.epm.util.M;
import jem.epm.util.ParserException;
import jem.epm.util.config.EpmConfig;
import lombok.val;
import pw.phylame.commons.io.BufferedRandomAccessFile;
import pw.phylame.commons.io.ByteUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public abstract class BinaryParser<C extends EpmConfig> extends AbstractParser<RandomAccessFile, C> {

    protected BinaryParser(String name, Class<C> clazz) {
        super(name, clazz);
    }

    @Override
    protected RandomAccessFile openInput(File file, C config) throws IOException {
        return new BufferedRandomAccessFile(file, "r");
    }

    protected void onBadInput() throws ParserException {
        throw new ParserException(M.tr("err.parse.badInput"));
    }

    protected void onBadInput(String key, Object... args) throws ParserException {
        throw new ParserException(M.tr(key, args));
    }

    protected byte[] readData(RandomAccessFile input, int size) throws IOException, ParserException {
        return readData(input, size, null);
    }

    protected byte[] readData(RandomAccessFile input, int size, String key, Object... args)
            throws IOException, ParserException {
        if (size < 0) {
            throw E.forIllegalArgument("size(%d) < 0", size);
        }
        val data = new byte[size];
        if (input.read(data) != size) {
            if (key != null) {
                onBadInput(key, args);
            } else {
                onBadInput();
            }
        }
        return data;
    }

    protected int readUInt16(RandomAccessFile input) throws IOException, ParserException {
        return ByteUtils.littleParser.getUInt16(readData(input, 2), 0);
    }

    protected long readUInt32(RandomAccessFile input) throws IOException, ParserException {
        return ByteUtils.littleParser.getUInt32(readData(input, 4), 0);
    }

    protected String readString(RandomAccessFile input, int length, String encoding)
            throws ParserException, IOException {
        return new String(readData(input, length), encoding);
    }
}