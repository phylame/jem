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

import pw.phylame.commons.util.Implementor;
import pw.phylame.commons.util.MiscUtils;

public final class ProviderManager {
    private ProviderManager() {
    }

    public static final String AUTO_LOAD_KEY = "jem.crawler.autoLoad";

    public static final String REGISTRY_FILE = "META-INF/jem/crawler-providers.prop";

    private static final Implementor<CrawlerProvider> providers = new Implementor<>(CrawlerProvider.class, false);

    public static void register(String host, Class<? extends CrawlerProvider> clazz) {
        providers.register(host, clazz);
    }

    public static void register(String host, String path) {
        providers.register(host, path);
    }

    public static void unregister(String host) {
        providers.remove(host);
    }

    public static String[] knownHosts() {
        return providers.names();
    }

    public static boolean isRegistered(String host) {
        return providers.contains(host);
    }

    public static CrawlerProvider providerFor(String host)
            throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        return providers.getInstance(host);
    }

    public static void loadProviders() {
        providers.load(REGISTRY_FILE, MiscUtils.getContextClassLoader(), null);
    }

    static {
        if (Boolean.getBoolean(AUTO_LOAD_KEY)) {
            loadProviders();
        }
    }
}
