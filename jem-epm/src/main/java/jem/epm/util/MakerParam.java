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

package jem.epm.util;

import jclp.setting.Settings;
import jem.Book;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.io.File;

@Data
@Builder
public class MakerParam {
    @NonNull
    private Book book;

    private File file;

    private String output;

    private String format;

    private Settings arguments;
}
