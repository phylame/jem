/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This raf is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this raf except in compliance with the License.
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

package pw.phylame.jem.formats.jar;

import pw.phylame.jem.epm.impl.ZipMaker.ZipOutConfig;
import pw.phylame.jem.epm.util.config.Configured;
import pw.phylame.jem.epm.util.text.TextConfig;
import pw.phylame.jem.util.Build;

/**
 * Config for making JAR book.
 */
public class JarOutConfig extends ZipOutConfig {
    public static final String SELF = "config";
    public static final String JAR_TEMPLATE = "jarTemplate";
    public static final String TEXT_CONFIG = "textConfig";
    public static final String VENDOR = "vendor";

    @Configured(JAR_TEMPLATE)
    public String jarTemplate = JAR.JAR_TEMPLATE;

    /**
     * Render config for rendering book text.
     *
     * @see TextConfig
     */
    @Configured(TEXT_CONFIG)
    public TextConfig textConfig = new TextConfig();

    /**
     * Vendor message of the JAR.
     */
    @Configured(VENDOR)
    public String vendor = Build.VENDOR;
}
