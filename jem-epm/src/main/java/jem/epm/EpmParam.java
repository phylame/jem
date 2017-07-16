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

package jem.epm;

import jclp.io.PathUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.io.File;
import java.util.Map;

import static jclp.util.StringUtils.coalesce;
import static jclp.util.StringUtils.isNotEmpty;

@Data
@RequiredArgsConstructor
abstract class EpmParam {
    /**
     * Input/output file
     */
    private final File file;

    /**
     * Input/output path
     */
    private final String path;

    /**
     * Name of epm worker
     */
    private final String format;

    /**
     * Epm worker arguments.
     */
    private final Map<String, Object> arguments;

    public final String getFormat() {
        if (isNotEmpty(format)) {
            return format;
        }
        val suffix = PathUtils.extName(isNotEmpty(path) ? path : file.getPath());
        return coalesce(EpmManager.nameOfExtension(suffix), suffix);
    }
}
