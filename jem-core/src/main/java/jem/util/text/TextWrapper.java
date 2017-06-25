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

package jem.util.text;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
public class TextWrapper implements Text {
    @NonNull
    private final Text actual;

    public final Text getTarget() {
        return actual;
    }

    @Override
    public Iterator<String> iterator() {
        return actual.iterator();
    }

    @Override
    public String getType() {
        return actual.getType();
    }

    @Override
    public String getText() {
        return actual.getText();
    }

    @Override
    public List<String> getLines(boolean skipEmpty) {
        return actual.getLines(skipEmpty);
    }

    @Override
    public long writeTo(Writer writer) throws IOException {
        return actual.writeTo(writer);
    }
}
