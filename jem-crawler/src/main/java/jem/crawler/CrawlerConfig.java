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

package jem.crawler;

import java.util.concurrent.ExecutorService;

import jem.epm.util.config.AbstractConfig;
import jem.epm.util.config.Configured;
import pw.phylame.commons.cache.Cacheable;

public class CrawlerConfig extends AbstractConfig {
    public static final String SELF = "config";
    public static final String TRY_COUNT = "tryCount";
    public static final String TIMEOUT = "timeout";
    public static final String CACHE = "cache";
    public static final String LISTENER = "listener";
    public static final String EXECUTOR = "executor";
    public static final String LINE_SEPARATOR = "lineSeparator";

    /**
     * Timeout for networks.
     */
    @Configured(TIMEOUT)
    public int timeout = 10000;

    /**
     * Count for retry when connection timeout.
     */
    @Configured(TRY_COUNT)
    public int tryCount = 5;

    /**
     * The text cache.
     */
    @Configured(CACHE)
    public Cacheable cache = null;

    @Configured(LISTENER)
    public CrawlerListener listener = null;

    @Configured(EXECUTOR)
    public ExecutorService executor = null;

    @Configured(LINE_SEPARATOR)
    public String lineSeparator = System.lineSeparator();
}
