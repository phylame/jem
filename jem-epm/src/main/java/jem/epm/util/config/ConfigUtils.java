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

package jem.epm.util.config;

import jem.epm.util.E;
import jem.epm.util.M;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import pw.phylame.commons.format.Converters;
import pw.phylame.commons.function.Prediction;
import pw.phylame.commons.log.Log;
import pw.phylame.commons.util.CollectionUtils;
import pw.phylame.commons.util.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * Utilities for maker and parser configurations
 */
public final class ConfigUtils {
    private ConfigUtils() {
    }

    public static final String TAG = "CFG";

    @SneakyThrows({InstantiationException.class, IllegalAccessException.class})
    public static <C extends EpmConfig> C defaultConfig(@NonNull Class<C> clazz) {
        return clazz.newInstance();
    }

    public static <C extends EpmConfig> C fetchConfig(Map<String, Object> m, String prefix, @NonNull Class<C> clazz)
            throws BadConfigException {
        if (CollectionUtils.isEmpty(m)) {
            return defaultConfig(clazz);
        }
        C config = null;
        try {
            // get config object in m, key: prefix + <Class>.SELF
            val field = clazz.getField(EpmConfig.SELF_FIELD_NAME);
            if (Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                // find the config object by key
                config = fetchObject(m, (prefix != null ? prefix : "") + field.get(null), clazz, null);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.d(TAG, "cannot get config by '{0}' field", EpmConfig.SELF_FIELD_NAME);
        }
        if (config != null) {
            return config;
        }
        config = defaultConfig(clazz);
        fetchFields(config, m, prefix);
        return config;
    }

    @SuppressWarnings("unchecked")
    public static <T> T fetchObject(Map<String, Object> m, String key, Class<T> type, Object fallback)
            throws BadConfigException {
        val obj = m.get(key);
        if (obj == null) {
            return m.containsKey(key) ? null : (T) fallback;
        }
        if (type.isInstance(obj)) { // found the item
            return (T) obj;
        }
        if (obj instanceof String) {
            val value = Converters.parse((String) obj, type);
            if (value != null) {
                return value;
            }
        }
        throw E.forBadConfig(key, obj, type.getName());
    }

    @SuppressWarnings("unchecked")
    private static void fetchFields(EpmConfig config, Map<String, Object> m, String prefix) throws BadConfigException {
        val fields = Reflections.getFields(config.getClass(), new Prediction<Field>() {
            @Override
            public boolean test(Field field) {
                int mod = field.getModifiers();
                return Modifier.isPublic(mod) && !Modifier.isStatic(mod);
            }
        });
        for (val field : fields) {
            val configured = field.getAnnotation(Configured.class);
            if (configured == null) {
                Log.d(TAG, "field {0} is not configured", field.getName());
                continue;
            }
            String key = configured.value();
            if (prefix != null) {
                key = prefix + key;
            }
            Log.t(TAG, "key for field {0} is {1}", field.getName(), key);
            val type = field.getType();
            try {
                Object value = fetchObject(m, key, type, null);
                if (value == null) { // not found in m
                    Object initial = field.get(config);
                    if (EpmConfig.class.isAssignableFrom(type)) { // field is EpmConfig
                        fetchFields(initial != null
                                ? (EpmConfig) initial
                                : defaultConfig((Class<? extends EpmConfig>) type), m, prefix);
                    }
                    value = initial;
                }
                field.set(config, value);
            } catch (IllegalAccessException e) {
                throw E.forBadConfig(key, null,
                        M.tr("err.config.inaccessible", config.getClass(), field.getName(), e.getMessage()));
            }
        }
        if (!fields.isEmpty() && config instanceof AdjustableConfig) {
            ((AdjustableConfig) config).adjust();
        }
    }
}
