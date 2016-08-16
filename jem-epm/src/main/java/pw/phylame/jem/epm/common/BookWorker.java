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

package pw.phylame.jem.epm.common;

import lombok.Getter;
import lombok.NonNull;
import pw.phylame.jem.epm.util.config.Config;
import pw.phylame.ycl.util.Validate;

import java.util.Map;

class BookWorker<C extends Config> {
    @Getter
    private final String name;

    private final String cfgkey;

    private final Class<C> cfgcls;

    BookWorker(@NonNull String name, String cfgkey, Class<C> cfgcls) {
        this.name = name;
        Validate.require(cfgkey == null || cfgcls != null, "'cfgkey' is not null but 'cfgcls' is null");
        this.cfgkey = cfgkey;
        this.cfgcls = cfgcls;
    }

    protected final C fetchConfig(Map<String, Object> m) {
        if (cfgkey == null) { // no config required

        }
        return null;
    }
}
