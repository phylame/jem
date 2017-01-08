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

package pw.phylame.jem.crawler;

import pw.phylame.jem.epm.util.config.AbstractConfig;
import pw.phylame.jem.epm.util.config.Configured;

public class CrawlerConfig extends AbstractConfig {
    public static final String SELF = "config";
    public static final String TIMEOUT = "timeout";
    public static final String LINE_SEPARATOR = "lineSeparator";
    public static final String FETCH_LISTENER = "fetchListener";

    /**
     * Timeout for networks.
     */
    @Configured(TIMEOUT)
    public int timeout = 3000;

    @Configured(LINE_SEPARATOR)
    public String lineSeparator = System.lineSeparator();

    @Configured(FETCH_LISTENER)
    public OnFetchingListener fetchingListener;
}
