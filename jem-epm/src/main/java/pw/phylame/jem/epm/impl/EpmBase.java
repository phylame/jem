/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pw.phylame.jem.epm.impl;

import lombok.Getter;
import pw.phylame.jem.epm.Parser;
import pw.phylame.jem.epm.util.config.BadConfigException;
import pw.phylame.jem.epm.util.config.ConfigUtils;
import pw.phylame.jem.epm.util.config.EpmConfig;
import pw.phylame.ycl.util.StringUtils;
import pw.phylame.ycl.util.Validate;

import java.util.Map;

/**
 * Implementation base for parser and maker.
 *
 * @param <C> the config type
 */
public abstract class EpmBase<C extends EpmConfig> {
    /**
     * Key for storing meta data of book in extensions.
     * <p></p><strong>NOTE: </strong>the corresponding item should be ignored by {@code Maker}.
     */
    public static final String META_KEY = "jem.ext.meta";

    /**
     * Name of format for the parser or maker.
     */
    @Getter
    private final String name;

    /**
     * Class of the config.
     * <p>If no config required, set to {@literal null}.
     */
    private final Class<C> clazz;

    protected EpmBase(String name, Class<C> clazz) {
        Validate.require(StringUtils.isNotEmpty(name), "name cannot be null or empty");
        this.name = name;
        this.clazz = clazz;
    }

    protected final C fetchConfig(Map<String, Object> m) throws BadConfigException {
        return clazz == null
                ? null
                : ConfigUtils.fetchConfig(m, name + '.' + (this instanceof Parser ? "parse." : "make."), clazz);
    }
}
