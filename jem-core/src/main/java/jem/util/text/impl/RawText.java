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

package jem.util.text.impl;

import jem.util.Variants;
import jem.util.text.AbstractText;
import lombok.NonNull;

public class RawText extends AbstractText {
    static {
        Variants.mapClass(Variants.TEXT, RawText.class);
    }

    private final CharSequence text;

    public RawText(String type, @NonNull CharSequence text) {
        super(type);
        this.text = text;
    }

    @Override
    public String toString() {
        return text.toString();
    }
}
