/*
 * Copyright 2016 Peng Wan <phylame@163.com>
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

package pw.phylame.jem.epm.util;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.core.Cleanable;
import pw.phylame.ycl.io.IOUtils;

import java.io.Closeable;

@AllArgsConstructor
public class SourceCleaner implements Cleanable {
    @NonNull
    private final Closeable in;

    private final Runnable addon;

    public SourceCleaner(Closeable in) {
        this(in, null);
    }

    @Override
    public void clean(Chapter chapter) {
        IOUtils.closeQuietly(in);
        if (addon != null) {
            addon.run();
        }
    }
}
