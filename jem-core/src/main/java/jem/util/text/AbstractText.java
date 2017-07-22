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

package jem.util.text;

import jclp.util.StringUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import static jclp.util.Validate.requireNotEmpty;

public abstract class AbstractText implements Text {
    @Getter
    private String type;

    protected AbstractText(String type) {
        requireNotEmpty(type, "type cannot be null or empty");
        this.type = type;
    }

    @Override
    public abstract String toString();

    @Override
    public Iterator<String> iterator() {
        return StringUtils.splitLines(toString(), false).iterator();
    }

    @Override
    public long writeTo(@NonNull Writer writer) throws IOException {
        val text = toString();
        writer.write(text);
        return text.length();
    }
}
