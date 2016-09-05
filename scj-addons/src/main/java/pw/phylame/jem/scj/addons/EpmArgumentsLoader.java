/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of SCJ.
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

package pw.phylame.jem.scj.addons;

import java.io.IOException;
import java.util.Map;

import lombok.SneakyThrows;
import lombok.val;
import pw.phylame.jem.scj.app.AppConfig;
import pw.phylame.ycl.util.MiscUtils;

public class EpmArgumentsLoader extends AbstractPlugin {

    private static final String NAME_SUFFIX = ".prop";

    public EpmArgumentsLoader() {
        super(new Metadata("ee4ef607-a500-4e16-aecc-08aa457a60ea", "Epm Arguments Loader", "1.0", "PW"));
    }

    @Override
    public void init() {
        val cfg = AppConfig.INSTANCE;
        update("in-args", cfg.getInArguments());
        update("out-attrs", cfg.getOutAttributes());
        update("out-exts", cfg.getOutExtensions());
        update("out-args", cfg.getOutArguments());
    }

    @SneakyThrows(IOException.class)
    private void update(String name, Map<String, Object> m) {
        val prop = MiscUtils.propertiesFor(app.pathOf(name + NAME_SUFFIX), getClass().getClassLoader());
        if (prop != null) {
            for (val e : prop.entrySet()) {
                m.put(e.getKey().toString(), e.getValue());
            }
        }
    }
}
