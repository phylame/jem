/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pw.phylame.jem.util;

import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.jem.util.text.Text;
import pw.phylame.jem.util.text.Texts;
import pw.phylame.ycl.io.PathUtils;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.CollectUtils;
import pw.phylame.ycl.util.Exceptions;
import pw.phylame.ycl.util.Provider;
import pw.phylame.ycl.util.StringUtils;
import pw.phylame.ycl.value.Lazy;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class Variants {
    private Variants() {
    }

    // declare variant type aliases
    public static final String FLOB = "file";
    public static final String TEXT = "text";
    public static final String STRING = "str";
    public static final String INTEGER = "int";
    public static final String REAL = "real";
    public static final String LOCALE = "locale";
    public static final String DATETIME = "datetime";
    public static final String BOOLEAN = "bool";

    private static Collection<String> typeNames = new LinkedHashSet<>();

    private static final Map<Class<?>, String> typeMap = new ConcurrentHashMap<>();

    private static final String TYPE_MAP_PATH = "!pw/phylame/jem/util/variants.properties";

    static {
        try {
            val prop = CollectUtils.propertiesFor(TYPE_MAP_PATH);
            if (prop != null) {
                for (val e : prop.entrySet()) {
                    typeMap.put(Class.forName(e.getKey().toString()), e.getValue().toString());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Log.e("Variants", e);
        }

        typeNames.addAll(typeMap.values());
    }

    /**
     * Returns supported type by Jem.
     *
     * @return array of type names
     */
    public static String[] supportedTypes() {
        return typeNames.toArray(new String[typeNames.size()]);
    }

    /**
     * Registers user's type.
     *
     * @param type name of type
     */
    public static void registerType(String type) {
        if (StringUtils.isEmpty(type)) {
            throw new IllegalArgumentException("type cannot be null or empty");
        }
        if (typeNames.contains(type)) {
            throw Exceptions.forIllegalArgument("type %s registered", type);
        }
        typeNames.add(type);
    }

    private static void checkTypeName(String type) {
        if (StringUtils.isEmpty(type)) {
            throw new IllegalArgumentException("type cannot be null or empty");
        }
        if (!typeNames.contains(type)) {
            throw Exceptions.forIllegalArgument("type %s in unsupported", type);
        }
    }

    /**
     * Maps specified type for class.
     *
     * @param clazz the class
     * @param type  name of type
     * @throws NullPointerException     if the class is {@literal null}
     * @throws IllegalArgumentException if the name of type is empty
     */
    public static void mapType(@NonNull Class<?> clazz, String type) {
        checkTypeName(type);
        typeMap.put(clazz, type);
    }

    /**
     * Returns type name of specified object.
     *
     * @param obj the object
     * @return the type name or {@literal null} if unknown
     * @throws NullPointerException if the object is {@literal null}
     */
    public static String typeOf(@NonNull Object obj) {
        val type = typeMap.get(obj.getClass());
        if (type != null) {
            return type;
        }
        for (val e : typeMap.entrySet()) {
            if (e.getKey().isInstance(obj)) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Returns default value of specified type.
     *
     * @param type name of type
     * @return the value or {@literal null} if the type not in jem built-in types
     * @throws IllegalArgumentException if the name of type is empty
     */
    public static Object defaultFor(String type) {
        checkTypeName(type);
        switch (type) {
            case STRING:
                return StringUtils.EMPTY_TEXT;
            case TEXT:
                return Texts.forEmpty(Texts.PLAIN);
            case FLOB:
                return Flobs.forEmpty("_empty_", PathUtils.UNKNOWN_MIME);
            case DATETIME:
                return new Date();
            case LOCALE:
                return Locale.getDefault();
            case INTEGER:
                return 0;
            case REAL:
                return 0.0D;
            case BOOLEAN:
                return Boolean.FALSE;
            default:
                return null;
        }
    }

    private static final Lazy<DateFormat> shortDateFormatter = new Lazy<>(new Provider<DateFormat>() {
        @Override
        public DateFormat provide() throws Exception {
            return new SimpleDateFormat("yy-M-d");
        }
    });

    /**
     * Converts specified object to string for printing.
     *
     * @param obj the object or {@literal null} if the type of object is unknown
     * @return a string represent the object
     */
    public static String printable(@NonNull Object obj) {
        if (obj instanceof CharSequence) {
            return obj.toString();
        } else if (obj instanceof Text) {
            return ((Text) obj).getText();
        } else if (obj instanceof Date) {
            return shortDateFormatter.get().format((Date) obj);
        } else if (obj instanceof Locale) {
            return ((Locale) obj).getDisplayName();
        } else {
            return null;
        }
    }

    public static void main(String[] args) throws ClassNotFoundException {
        for (Map.Entry<Class<?>, String> entry : typeMap.entrySet()) {
            System.out.println(entry.getKey().getName() + '=' + entry.getValue());
        }
    }
}
