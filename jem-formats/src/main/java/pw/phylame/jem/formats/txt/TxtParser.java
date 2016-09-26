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

package pw.phylame.jem.formats.txt;

import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.epm.base.AbstractParser;
import pw.phylame.jem.epm.util.FileDeleter;
import pw.phylame.jem.epm.util.ParserException;
import pw.phylame.jem.formats.util.M;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.jem.util.text.Text;
import pw.phylame.jem.util.text.Texts;
import pw.phylame.ycl.io.BufferedRandomAccessFile;
import pw.phylame.ycl.io.IOUtils;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.StringUtils;
import pw.phylame.ycl.value.Triple;

import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * <tt>Parser</tt> implement for TXT book.
 */
public class TxtParser extends AbstractParser<Reader, TxtInConfig> {
    private static final String CACHE_ENCODING = "UTF-16";

    public TxtParser() {
        super("txt", TxtInConfig.class);
    }

    @Override
    protected Reader open(File file, TxtInConfig config) throws IOException {
        val in = new FileInputStream(file);
        try {
            return new BufferedReader(new InputStreamReader(in, config.encoding));
        } catch (UnsupportedEncodingException e) {
            in.close();
            throw e;
        }
    }

    @Override
    public Book parse(@NonNull Reader input, TxtInConfig config) throws IOException, ParserException {
        if (config == null) {
            config = new TxtInConfig();
        }
        return parse(input, config.title, config);
    }

    public Book parse(@NonNull Reader reader, @NonNull String title, TxtInConfig config) throws IOException, ParserException {
        if (config == null) {
            config = new TxtInConfig();
        }
        Pattern pattern;
        try {
            pattern = Pattern.compile(config.pattern, config.patternFlags);
        } catch (PatternSyntaxException e) {
            throw new ParserException(M.tr("txt.parse.invalidPattern", config.pattern), e);
        }

        val triple = cacheContent(reader);
        reader.close();

        val source = triple.getFirst();
        val cache = triple.getSecond();

        val book = new Book(title, "");
        try {
            val matcher = pattern.matcher(triple.getThird());

            int prevOffset, firstOffset;
            Flobs.BlockFlob flob;
            if (matcher.find()) {
                firstOffset = prevOffset = matcher.start();
                title = matcher.group();
                if (config.trimChapterTitle) {
                    prevOffset += title.length();
                }
                flob = Flobs.forBlock("0.txt", source, prevOffset << 1, 0, TXT.MIME_PLAIN_TEXT);
                book.append(new Chapter(StringUtils.trimmed(title), Texts.forFile(flob, CACHE_ENCODING, Text.PLAIN)));
            } else {
                source.close();
                if (!cache.delete()) {
                    Log.e(getName(), "Failed to delete TXT cache: %s", cache);
                }
                return book;
            }
            while (matcher.find()) {
                int offset = matcher.start();
                flob.size = (offset - prevOffset) << 1;

                title = matcher.group();
                if (config.trimChapterTitle) {
                    offset += title.length();
                }

                flob = Flobs.forBlock(book.size() + ".txt", source, offset << 1, 0, TXT.MIME_PLAIN_TEXT);
                prevOffset = offset;
                book.append(new Chapter(StringUtils.trimmed(title), Texts.forFile(flob, CACHE_ENCODING, Text.PLAIN)));
            }
            flob.size = (triple.getThird().length() - prevOffset) << 1;

            if (firstOffset > 0) {    // no formatted head store as intro
                flob = Flobs.forBlock("head.txt", source, 0, firstOffset << 1, TXT.MIME_PLAIN_TEXT);
                Attributes.setIntro(book, Texts.forFile(flob, CACHE_ENCODING, Text.PLAIN));
            }
        } catch (IOException e) {
            source.close();
            if (!cache.delete()) {
                Log.e(getName(), "Failed to delete TXT cache: %s", cache);
            }
            throw e;
        }
        book.registerCleanup(new FileDeleter(source, cache));
        System.gc();
        return book;
    }

    private Triple<RandomAccessFile, File, String> cacheContent(Reader reader) throws IOException {
        val b = new StringBuilder();
        val cache = File.createTempFile("jem_txt_", ".tmp");
        Closeable closeable = null;
        try {
            OutputStream out = new FileOutputStream(cache);
            closeable = out;
            Writer writer = new OutputStreamWriter(out, CACHE_ENCODING);
            closeable = out;
            writer = new BufferedWriter(writer);
            closeable = writer;

            val buf = new char[4096];
            int num;
            while ((num = reader.read(buf)) != -1) {
                writer.write(buf, 0, num);
                b.append(buf, 0, num);
            }
            writer.close();
            closeable = null;
            return new Triple<>(new BufferedRandomAccessFile(cache, "r"), cache, b.toString());
        } catch (IOException e) {
            IOUtils.closeQuietly(closeable);
            if (!cache.delete()) {
                Log.e(getName(), "Failed to delete TXT cache: %s", cache);
            }
            throw e;
        }
    }
}
