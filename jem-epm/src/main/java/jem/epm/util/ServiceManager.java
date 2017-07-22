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

package jem.epm.util;

import lombok.NonNull;
import lombok.val;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

public class ServiceManager<T> {
    private static final boolean DEBUG = false;

    private final Class<T> serviceType;
    private final ClassLoader classLoader;
    private ServiceLoader<T> serviceLoader;
    private Set<T> serviceSpis;

    public ServiceManager(@NonNull Class<T> serviceType) {
        this.serviceType = serviceType;
        this.classLoader = Thread.currentThread().getContextClassLoader();
        init();
    }

    public ServiceManager(@NonNull Class<T> serviceType, ClassLoader loader) {
        this.serviceType = serviceType;
        this.classLoader = loader;
        init();
    }

    protected void init() {
        serviceSpis = new HashSet<>();
        serviceLoader = AccessController.doPrivileged(new PrivilegedAction<ServiceLoader<T>>() {
            @Override
            public ServiceLoader<T> run() {
                return classLoader != null
                        ? ServiceLoader.load(serviceType, classLoader)
                        : ServiceLoader.loadInstalled(serviceType);
            }
        });
        initServices();
    }

    public void reload() {
        serviceSpis.clear();
        serviceLoader.reload();
        initServices();
    }

    public final Set<T> getServices() {
        return Collections.unmodifiableSet(serviceSpis);
    }

    private void initServices() {
        val it = serviceLoader.iterator();
        try {
            while (it.hasNext()) {
                try {
                    serviceSpis.add(it.next());
                } catch (ServiceConfigurationError e) {
                    System.err.println("ServiceManager providers.next(): " + e.getMessage());
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (ServiceConfigurationError e) {
            System.err.println("ServiceManager providers.hasNext(): " + e.getMessage());
            if (DEBUG) {
                e.printStackTrace();
            }
        }
    }
}
