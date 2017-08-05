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

package jem.crawler;

import jclp.function.Function;
import jem.util.text.Text;
import jem.util.text.Texts;
import lombok.NonNull;
import lombok.val;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static jclp.util.CollectionUtils.listOf;
import static jclp.util.StringUtils.join;
import static jclp.util.Validate.requireNotEmpty;

public class HtmlText implements Text, Callable<Iterator<String>> {
    private String url;
    private Function<String, Iterator<String>> parser;

    public HtmlText(String url, @NonNull Function<String, Iterator<String>> parser) {
        this.parser = parser;
        requireNotEmpty(url, "url cannot be null or empty");
        this.url = url;
    }

    @Override
    public String getType() {
        return Texts.PLAIN;
    }

    @Override
    public String getText() {
        return join(System.lineSeparator(), iterator());
    }

    @Override
    public List<String> getLines(boolean skipEmpty) {
        return listOf(iterator());
    }

    @Override
    public long writeTo(Writer writer) throws IOException {
        long chars = 0;
        for (val line : this) {
            writer.append(line).append(System.lineSeparator());
            chars += line.length();
        }
        return chars;
    }

    @Override
    public Iterator<String> iterator() {
        List<String> lines = this.lines.get();
        if (lines != null) {
            return lines.iterator();
        }
        Future<Iterator<String>> future = this.future.get();
        if (future != null) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                lines = Collections.emptyList();
            }
        } else {
            this.lines.set(lines = Collections.emptyList());
        }
        return lines.iterator();
    }

    private AtomicReference<Future<Iterator<String>>> future = new AtomicReference<>();

    private AtomicReference<List<String>> lines = new AtomicReference<>();

    @Override
    public Iterator<String> call() throws Exception {
        Iterator<String> iterator = parser.apply(url);
        lines.set(listOf(iterator));
        return iterator;
    }
}
