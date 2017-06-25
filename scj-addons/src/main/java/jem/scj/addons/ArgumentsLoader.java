/*
 * Copyright 2014-2017 Peng Wan <phylame@163.com>
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

package jem.scj.addons;

import jem.scj.app.AppConfig;
import jem.scj.app.SCJPlugin;
import jclp.log.Log;
import qaf.core.App;
import qaf.core.Metadata;

import java.io.IOException;
import java.util.Map;

import static jclp.util.CollectionUtils.propertiesFor;
import static jclp.util.CollectionUtils.update;

public class ArgumentsLoader extends SCJPlugin {
    private static final String TAG = ArgumentsLoader.class.getSimpleName();

    private static final String NAME_SUFFIX = ".prop";

    private App app = getApp();
    private AppConfig config = getConfig();

    public ArgumentsLoader() {
        super(new Metadata("ee4ef607-a500-4e16-aecc-08aa457a60ea", "Arguments Loader", "1.0", "PW"));
    }

    @Override
    public void init() {
        updateMap("in-arguments", config.getInArguments());
        updateMap("out-attributes", config.getOutAttributes());
        updateMap("out-extensions", config.getOutExtensions());
        updateMap("out-arguments", config.getOutArguments());
    }

    private void updateMap(String name, Map<String, Object> m) {
        try {
            update(m, propertiesFor(app.pathOf(name + NAME_SUFFIX), getClass().getClassLoader()));
        } catch (IOException e) {
            Log.e(TAG, "cannot load arguments", e);
        }
    }
}
