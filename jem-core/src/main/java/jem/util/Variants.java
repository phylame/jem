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

package jem.util;

import jem.util.flob.Flob;
import jem.util.flob.Flobs;
import jem.util.text.Text;
import jem.util.text.Texts;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import jclp.function.Function;
import jclp.function.Provider;
import jclp.io.PathUtils;
import jclp.log.Log;
import jclp.util.DateUtils;
import jclp.util.Validate;

import java.io.IOException;
import java.util.*;

import static jclp.util.CollectionUtils.getOrPut;
import static jclp.util.CollectionUtils.propertiesFor;
import static jclp.value.Values.*;

/**
 * Utilities for variants used by Jem.
 */
public final class Variants {
    private Variants() {
    }

    private static final String TAG = "Variants";

    // declare standard variant type aliases
    public static final String FLOB = "file";
    public static final String TEXT = "text";
    public static final String STRING = "str";
    public static final String INTEGER = "int";
    public static final String REAL = "real";
    public static final String BOOLEAN = "bool";
    public static final String LOCALE = "locale";
    public static final String DATETIME = "datetime";

    // names of registered variants
    private static final Set<String> names = new HashSet<>();

    // default value for variant name
    private static final Map<String, Object> defaults = new HashMap<>();

    // variant name for java class
    private static final Map<Class<?>, String> mappings = new IdentityHashMap<>();

    /**
     * Loads built-in variant mapping.
     */
    private static void initVariants() {
        Collections.addAll(names, FLOB, TEXT, STRING, INTEGER, REAL, BOOLEAN, LOCALE, DATETIME);
        defaults.put(FLOB, lazy(new Provider<Flob>() {
            @Override
            public Flob provide() throws Exception {
                return Flobs.forEmpty("_empty_", PathUtils.UNKNOWN_MIME);
            }
        }));
        defaults.put(TEXT, lazy(new Provider<Text>() {
            @Override
            public Text provide() throws Exception {
                return Texts.forEmpty(Texts.PLAIN);
            }
        }));
        defaults.put(BOOLEAN, Boolean.FALSE);
        defaults.put(STRING, "");
        defaults.put(INTEGER, 0);
        defaults.put(REAL, 0.0D);
        defaults.put(LOCALE, provider(new Provider<Locale>() {
            @Override
            public Locale provide() throws Exception {
                return Locale.getDefault();
            }
        }));
        defaults.put(DATETIME, provider(new Provider<Date>() {
            @Override
            public Date provide() throws Exception {
                return new Date();
            }
        }));
        try {
            val prop = propertiesFor("!jem/util/variants.properties");
            if (prop != null) {
                String name;
                for (val e : prop.entrySet()) {
                    name = e.getValue().toString();
                    mapClass(Class.forName(e.getKey().toString()), name);
                    names.add(name);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, "error occurred when initialization", e);
        }
    }

    static {
        initVariants();
    }

    /**
     * Returns names of all registered variant type.
     *
     * @return set of type names
     */
    public static Set<String> supportedTypes() {
        return Collections.unmodifiableSet(names);
    }

    /**
     * Registers the specified variant type.
     *
     * @param type name of type
     * @throws IllegalArgumentException if the type is already registered
     */
    public static void registerType(String type) {
        Validate.requireNotEmpty(type, "type cannot be null or empty");
        Validate.require(!names.contains(type), "type %s already registered", type);
        names.add(type);
    }

    /**
     * Ensures specified variant type is registered.
     *
     * @param type name of type
     * @return the input type
     * @throws IllegalArgumentException if the type is not registered
     */
    public static String ensureRegistered(String type) {
        Validate.requireNotEmpty(type, "type cannot be null or empty");
        Validate.require(names.contains(type), "type %s in unsupported", type);
        return type;
    }

    /**
     * Gets readable title text for variant type.
     *
     * @param type name of type
     * @return the text, or {@literal null} if the type is unknown
     */
    public static String titleOf(@NonNull String type) {
        return M.optTr("variant." + type, null);
    }

    /**
     * Maps specified class for specified variant type.
     *
     * @param clazz the class
     * @param type  name of type
     * @throws IllegalArgumentException if the type is not registered
     */
    public static void mapClass(@NonNull Class<?> clazz, String type) {
        mappings.put(clazz, ensureRegistered(type));
    }

    /**
     * Maps specified value for variant type.
     * <p>The default value can be fetched by {@link #defaultOf(String)}</p>
     *
     * @param type  name of type
     * @param value the default value, may be instance {@link Provider} called when fetching defaults
     */
    public static void setDefault(@NonNull String type, Object value) {
        defaults.put(type, value);
    }

    /**
     * Detects the type of specified variant object.
     *
     * @param obj the object
     * @return the type name or {@literal null} if unknown
     * @throws NullPointerException if the object is {@literal null}
     */
    public static String typeOf(@NonNull Object obj) {
        return getOrPut(mappings, obj.getClass(), false, new Function<Class<?>, String>() {
            @Override
            public String apply(Class<?> clazz) {
                for (val e : mappings.entrySet()) {
                    if (e.getKey().isAssignableFrom(clazz)) {
                        return e.getValue();
                    }
                }
                return null;
            }
        });
    }

    /**
     * Returns default value for specified variant type.
     *
     * @param type name of type
     * @return the value, or {@literal null} if the type not in jem built-in types
     * @throws IllegalArgumentException if the name of type is empty
     */
    @SneakyThrows(Exception.class)
    public static Object defaultOf(String type) {
        return get(defaults.get(ensureRegistered(type)));
    }

    /**
     * Converts specified variant object to printable string.
     *
     * @param obj the object or {@literal null} if the type of object is unknown
     * @return a string represent the object
     */
    public static String printable(@NonNull Object obj) {
        val type = typeOf(obj);
        if (type == null) {
            return null;
        }
        switch (type) {
            case STRING:
            case BOOLEAN:
            case REAL:
            case INTEGER:
            case FLOB:
                return obj.toString();
            case TEXT:
                return ((Text) obj).getText();
            case DATETIME:
                return DateUtils.format((Date) obj, "yyyy-M-d");
            case LOCALE:
                return ((Locale) obj).getDisplayName();
            default:
                return null;
        }
    }
}
