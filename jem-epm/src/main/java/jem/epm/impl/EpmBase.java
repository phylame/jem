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

package jem.epm.impl;

import jem.epm.Parser;
import jem.epm.util.config.BadConfigException;
import jem.epm.util.config.ConfigUtils;
import jem.epm.util.config.EpmConfig;
import lombok.Getter;
import jclp.util.Validate;

import java.util.Map;

/**
 * Implementation base for parser and maker.
 *
 * @param <C> the config clazz
 */
public abstract class EpmBase<C extends EpmConfig> {

    /**
     * Name of format for the parser or maker.
     */
    @Getter
    private final String name;

    /**
     * Class of the config.
     * <p>
     * If no config required, set to {@literal null}.
     */
    private final Class<C> clazz;

    protected EpmBase(String name, Class<C> clazz) {
        Validate.requireNotEmpty(name, "name cannot be null or empty");
        this.name = name;
        this.clazz = clazz;
    }

    protected final C fetchConfig(Map<String, Object> m) throws BadConfigException {
        return clazz == null
                ? null
                : ConfigUtils.fetchConfig(m, name + '.' + (this instanceof Parser ? "parse." : "make."), clazz);
    }
}
