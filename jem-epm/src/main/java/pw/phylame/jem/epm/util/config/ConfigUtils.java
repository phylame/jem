/*
 * Copyright 2016 Peng Wan <phylame@163.com>
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

package pw.phylame.jem.epm.util.config;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import pw.phylame.jem.epm.util.Exceptions;
import pw.phylame.jem.epm.util.JEMessages;
import pw.phylame.ycl.format.Converters;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.MiscUtils;

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

    public static <C extends EpmConfig> C fetchConfig(Map<String, Object> m, String prefix, @NonNull Class<C> clazz) throws BadConfigException {
        if (MiscUtils.isEmpty(m)) {
            return defaultConfig(clazz);
        }
        C config = null;
        try {
            // get config object in m, key: prefix + <Class>.SELF
            val field = clazz.getField(EpmConfig.SELF_FIELD_NAME);
            if (Modifier.isStatic(field.getModifiers())) {
                config = fetchObject(m, (prefix != null ? prefix : "") + field.get(null), clazz, null); // find the config object by key
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.d(TAG, "cannot get config by {0} field", EpmConfig.SELF_FIELD_NAME);
        }
        if (config != null) {
            return config;
        }
        config = defaultConfig(clazz);
        fetchFields(config, m, prefix);
        return config;
    }

    @SuppressWarnings("unchecked")
    public static <T> T fetchObject(Map<String, Object> m, String key, Class<T> type, Object fallback) throws BadConfigException {
        Object o = m.get(key);
        if (o == null) {
            return m.containsKey(key) ? null : (T) fallback;
        }
        if (type.isInstance(o)) {   // found the item
            return (T) o;
        }
        if (o instanceof String) {
            val value = Converters.parse((String) o, type);
            if (value != null) {
                return value;
            }
        }
        throw Exceptions.forBadConfig(key, o, type.getName());
    }

    @SuppressWarnings("unchecked")
    private static void fetchFields(EpmConfig config, Map<String, Object> m, String prefix) throws BadConfigException {
        Field[] fields = config.getClass().getFields();
        for (Field field : fields) {
            val mapped = field.getAnnotation(Mapped.class);
            if (mapped == null) {
                Log.i(TAG, "field {0} is not mapped", field.getName());
                continue;
            }
            if (Modifier.isStatic(field.getModifiers())) {
                Log.i(TAG, "field {0} is static", field.getName());
                throw new BadConfigException(mapped.value(), null,
                        JEMessages.tr("err.config.inaccessible", field.getName(), config.getClass(), "static field is not permitted with Mapped"));
            }
            String key = mapped.value();
            if (prefix != null) {
                key = prefix + key;
            }
            Log.i(TAG, "key for field {0} is {1}", field.getName(), key);
            Class<?> type = field.getType();
            try {
                Object value = fetchObject(m, key, type, null);
                if (value == null) {    // not found in m
                    Object initial = field.get(config);
                    if (EpmConfig.class.isAssignableFrom(type)) {   // field is EpmConfig
                        fetchFields(initial != null
                                ? (EpmConfig) initial
                                : defaultConfig((Class<? extends EpmConfig>) type), m, prefix);
                    }
                    value = initial;
                }
                field.set(config, value);
            } catch (IllegalAccessException e) {
                throw Exceptions.forBadConfig(key, null,
                        JEMessages.tr("err.config.inaccessible", config.getClass(), field.getName(), e.getMessage()));
            }
        }
        if (fields.length > 0 && config instanceof AdjustableConfig) {
            ((AdjustableConfig) config).adjust();
        }
    }
}
