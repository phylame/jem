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

package jem.epm;

import jem.core.Book;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.io.File;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class EpmOutParam extends EpmParam {
    /**
     * The book to write.
     */
    @NonNull
    private final Book book;

    public EpmOutParam(Book book, File file, String extension, Map<String, Object> arguments) {
        super(file, extension, arguments);
        this.book = book;
    }
}