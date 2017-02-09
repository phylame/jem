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

import lombok.*;
import pw.phylame.commons.io.PathUtils;
import pw.phylame.commons.util.StringUtils;

import java.io.File;
import java.util.Map;

@Data
@RequiredArgsConstructor
abstract class EpmParam {
    /**
     * The book file.
     */
    @NonNull
    private final File file;

    /**
     * Extension name of file.
     */
    private final String extension;

    /**
     * Name of epm maker.
     */
    @Getter(lazy = true)
    private final String format = detectFormat();

    /**
     * Epm worker arguments.
     */
    private final Map<String, Object> arguments;

    private String detectFormat() {
        val ext = StringUtils.isNotEmpty(extension)
                ? extension
                : PathUtils.extName(file.getPath()).toLowerCase();
        return StringUtils.notEmptyOr(EpmManager.nameOfExtension(ext), ext);
    }
}
