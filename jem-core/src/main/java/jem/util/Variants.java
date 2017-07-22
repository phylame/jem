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

package jem.util;

import jclp.function.Function;
import jclp.function.Provider;
import jclp.io.IOUtils;
import jclp.text.ConverterManager;
import jclp.text.Converters;
import jclp.text.Parser;
import jclp.value.Values;
import jem.util.flob.Flob;
import jem.util.flob.Flobs;
import jem.util.text.Text;
import jem.util.text.Texts;
import lombok.NonNull;
import lombok.val;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.*;

import static jclp.util.CollectionUtils.getOrPut;
import static jclp.util.Validate.requireNotNull;

/**
 * Utilities for values used in Jem.
 */
public final class Variants {
    private Variants() {
    }

    // standard type names
    public static final String FLOB = "file";
    public static final String TEXT = "text";
    public static final String STRING = "str";
    public static final String INTEGER = "int";
    public static final String REAL = "real";
    public static final String BOOLEAN = "bool";
    public static final String LOCALE = "locale";
    public static final String DATETIME = "datetime";

    private static Map<String, Class<?>> typeMappings = new HashMap<>();

    private static Map<Class<?>, String> classMappings = new IdentityHashMap<>();

    private static Map<String, Object> typeDefaults = new HashMap<>();

    /**
     * Returns a set containing all type names.
     *
     * @return set of type names
     */
    public static Set<String> getTypes() {
        return typeMappings.keySet();
    }

    /**
     * Associates the specified class with the specified type name.
     *
     * @param type  the type name to mapping
     * @param clazz the class to be mapped
     * @throws NullPointerException if the type name or class is null
     */
    public static void mapClass(@NonNull String type, @NonNull Class<?> clazz) {
        typeMappings.put(type, clazz);
        classMappings.put(clazz, type);
    }

    /**
     * Gets the class of specified type name.
     *
     * @param type the type name
     * @return class of the type, or {@literal null} if no class found
     * @throws NullPointerException if the value is null
     */
    public static Class<?> getClass(@NonNull String type) {
        return typeMappings.get(type);
    }

    /**
     * Gets the type name of specified value.
     *
     * @param value the value
     * @return the type name of specified value, or {@literal null} if type of the value is unknown
     * @throws NullPointerException if the value is null
     */
    public static String getType(@NonNull Object value) {
        return getOrPut(classMappings, value.getClass(), false, new Function<Class<?>, String>() {
            @Override
            public String apply(Class<?> clazz) {
                for (val entry : classMappings.entrySet()) {
                    if (entry.getKey().isAssignableFrom(clazz)) {
                        return entry.getValue();
                    }
                }
                return null;
            }
        });
    }

    /**
     * Associates the specified default value with the specified type name.
     * <p>The value can be instance of {@link jclp.value.Value}, so its {@code get()} method will be invoked
     * when querying defaults.</p>
     *
     * @param type  the type name to mapping
     * @param value the default value to be mapped
     * @throws NullPointerException if the type is null
     */
    public static void setDefault(@NonNull String type, Object value) {
        typeDefaults.put(type, value);
    }

    /**
     * Gets default value for specified type name.
     *
     * @param type the type name
     * @return the default value, or {@literal null} if the type is unknown or no default value found
     * @throws NullPointerException if the type is null
     */
    public static Object getDefault(@NonNull String type) {
        return Values.get(typeDefaults.get(type));
    }

    /**
     * Gets a readable text for specified type name.
     *
     * @param type the type name
     * @return a readable text, or {@literal null} if not text found
     * @throws NullPointerException if the type is null
     */
    public static String getTitle(@NonNull String type) {
        return M.translator().optTr("variant." + type, null);
    }

    /**
     * Makes a printable text for specified value.
     *
     * @param value the value to be made
     * @return a printable text for specified value, or {@literal null} if class of value is unknown
     * @throws NullPointerException if the value is null
     */
    public static String printable(@NonNull Object value) {
        val type = getType(value);
        if (type == null) {
            return null;
        }
        switch (type) {
            case STRING:
            case BOOLEAN:
            case REAL:
            case INTEGER:
            case TEXT:
            case FLOB:
                return value.toString();
            case DATETIME:
                return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format((Date) value);
            case LOCALE:
                return ((Locale) value).getDisplayName();
            default:
                return null;
        }
    }

    /**
     * Parses specified string value to object with specified type.
     * <p>This method is backed by JCLP Converter API</p>
     *
     * @param value the string value to be parsed
     * @param type  the type name of the value
     * @return the object value, or {@literal null} if the type is unknown
     * @throws NullPointerException if the value or type is null
     * @see jclp.text.Converters
     * @see jclp.text.ConverterManager
     */
    public static Object parse(@NonNull String value, @NonNull String type) {
        Class<?> clazz = getClass(type);
        if (clazz == null || CharSequence.class.isAssignableFrom(clazz)) {
            return value;
        } else if (Text.class.isAssignableFrom(clazz)) {
            clazz = Text.class;
        } else if (Flob.class.isAssignableFrom(clazz)) {
            clazz = Flob.class;
        }
        return Converters.parse(value, clazz);
    }

    private static void mapBuiltins() {
        mapClass(REAL, Float.class);
        mapClass(REAL, Double.class);
        mapClass(INTEGER, Byte.class);
        mapClass(INTEGER, Short.class);
        mapClass(INTEGER, Integer.class);
        mapClass(INTEGER, Long.class);
        mapClass(BOOLEAN, Boolean.class);
        mapClass(STRING, Character.class);
        mapClass(STRING, CharSequence.class);
        mapClass(DATETIME, Date.class);
        mapClass(LOCALE, Locale.class);
        mapClass(TEXT, Text.class);
        mapClass(FLOB, Flob.class);
    }

    private static void setDefaults() {
        setDefault(REAL, 0.0D);
        setDefault(INTEGER, 0);
        setDefault(STRING, "");
        setDefault(BOOLEAN, false);
        setDefault(DATETIME, Values.lazy(new Provider<Date>() {
            @Override
            public Date provide() throws Exception {
                return new Date();
            }
        }));

        setDefault(LOCALE, Values.lazy(new Provider<Locale>() {
            @Override
            public Locale provide() throws Exception {
                return Locale.getDefault();
            }
        }));
        setDefault(FLOB, null);
        setDefault(TEXT, null);
    }

    private static void registerParsers() {
        ConverterManager.registerParser(Text.class, new Parser<Text>() {
            @Override
            public Text parse(String str) {
                return Texts.forString(str, Texts.PLAIN);
            }
        });
        ConverterManager.registerParser(Flob.class, new Parser<Flob>() {
            @Override
            public Flob parse(String str) {
                URL url;
                try {
                    url = IOUtils.resourceFor(str);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("bad url " + str, e);
                }
                requireNotNull(url, "no such resource %s", str);
                return Flobs.forURL(url);
            }
        });
    }

    static {
        mapBuiltins();
        setDefaults();
        registerParsers();
    }
}
