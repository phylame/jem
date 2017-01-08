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

import pw.phylame.ycl.util.Implementor;

import java.net.MalformedURLException;
import java.net.URL;

public final class ProviderManager {
    private ProviderManager() {
    }

    public static final String AUTO_LOAD_CUSTOMIZED_KEY = "jem.crawler.autoLoad";

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

    public static CrawlerProvider providerForHost(String host) throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        return providers.getInstance(host);
    }

    public static CrawlerProvider providerForUrl(String url) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        final URL u;
        try {
            u = new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
        return providerForHost(u.getProtocol() + "://" + u.getHost());
    }

    public static String getAttrUrlById(CrawlerProvider provider, String bookId) {
        if (provider instanceof Identifiable) {
            return ((Identifiable) provider).attrUrlOf(bookId);
        }
        return null;
    }

    public static void loadCustomizedProviders() {
        providers.load(REGISTRY_FILE, null);
    }

    static {
        if (Boolean.getBoolean(AUTO_LOAD_CUSTOMIZED_KEY)) {
            loadCustomizedProviders();
        }
    }
}
