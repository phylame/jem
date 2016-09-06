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

import java.util.Map;

import pw.phylame.ycl.util.MiscUtils;

public class ArgumentsLoader extends AbstractPlugin {

    private static final String NAME_SUFFIX = ".prop";

    public ArgumentsLoader() {
        super(new Metadata("ee4ef607-a500-4e16-aecc-08aa457a60ea", "Arguments Loader", "1.0", "PW"));
    }

    @Override
    public void init() {
        update("in-arguments", config.getInArguments());
        update("out-attributes", config.getOutAttributes());
        update("out-extensions", config.getOutExtensions());
        update("out-arguments", config.getOutArguments());
    }

    private void update(String name, Map<String, Object> m) {
        MiscUtils.updateByProperties(m, app.pathOf(name + NAME_SUFFIX), getClass().getClassLoader());
    }
}
