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

import jclp.util.Validate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.io.File;
import java.util.Map;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class EpmInParam extends EpmParam {
    /**
     * Cached book file.
     */
    private File cache;

    public EpmInParam(@NonNull File file, Map<String, Object> arguments) {
        this(file, null, null, arguments);
    }

    public EpmInParam(@NonNull String input, Map<String, Object> arguments) {
        this(null, input, null, arguments);
    }

    public EpmInParam(@NonNull File file, String format, Map<String, Object> arguments) {
        this(file, null, format, arguments);
    }

    public EpmInParam(@NonNull String input, String format, Map<String, Object> arguments) {
        this(null, input, format, arguments);
    }

    public EpmInParam(File file, String path, String format, Map<String, Object> arguments) {
        super(file, path, format, arguments);
        if (file == null) {
            Validate.requireNotEmpty(path, "path cannot be empty");
        }
    }
}
