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

package jem;

import jclp.log.Log;
import jem.util.M;
import jem.util.VariantMap;
import jem.util.VariantMap.Validator;
import jem.util.Variants;
import lombok.NonNull;
import lombok.val;

import java.io.IOException;
import java.util.*;

import static jclp.util.CollectionUtils.propertiesFor;
import static jclp.util.CollectionUtils.update;
import static jclp.util.StringUtils.isNotEmpty;
import static jclp.util.StringUtils.join;
import static jclp.util.Validate.require;

/**
 * Constants and utilities for chapter attributes.
 */
public final class Attributes {
    private Attributes() {
    }

    private static final String TAG = "Attributes";

    // standard attributes
    public static final String AUTHOR = "author";
    public static final String COVER = "cover";
    public static final String DATE = "date";
    public static final String GENRE = "genre";
    public static final String INTRO = "intro";
    public static final String ISBN = "isbn";
    public static final String KEYWORDS = "keywords";
    public static final String LANGUAGE = "language";
    public static final String PRICE = "price";
    public static final String PUBDATE = "pubdate";
    public static final String PUBLISHER = "publisher";
    public static final String RIGHTS = "rights";
    public static final String SERIES = "series";
    public static final String STATE = "state";
    public static final String TITLE = "title";
    public static final String VENDOR = "vendor";
    public static final String WORDS = "words";

    public static final String VALUES_SEPARATOR = ";";

    private static Map<String, String> typeMappings = new HashMap<>();

    private static final Validator typeValidator = new Validator() {
        @Override
        public void validate(String name, Object value) throws IllegalArgumentException {
            val type = getType(name);
            if (type == null) {
                return;
            }
            val clazz = Variants.getClass(type);
            require(clazz.isAssignableFrom(value.getClass()), "attribute '%s' must be '%s'", name, clazz.getName());
        }
    };

    public static VariantMap newAttributes() {
        return new VariantMap(typeValidator);
    }

    /**
     * Returns a set containing known attribute names.
     *
     * @return set of attribute names
     */
    public static Set<String> getNames() {
        return typeMappings.keySet();
    }

    /**
     * Associates the specified class with the specified type name.
     *
     * @param type  the type name to mapping
     * @param clazz the class to be mapped
     * @throws NullPointerException if the attribute name or type name is null
     */
    public static void mapName(@NonNull String name, @NonNull String type) {
        typeMappings.put(name, type);
    }

    /**
     * Returns the type name for specified attribute name.
     *
     * @param name the attribute name
     * @return the type name, or {@literal null} if attribute name is unknown
     * @throws NullPointerException if specified attribute name is null
     */
    public static String getType(@NonNull String name) {
        return typeMappings.get(name);
    }

    /**
     * Gets a readable text for specified attribute name.
     *
     * @param name the attribute name
     * @return a readable text, or {@literal null} if not text found
     * @throws NullPointerException if specified attribute name is null
     */
    public static String getTitle(@NonNull String name) {
        return M.translator().optTr("attribute." + name, null);
    }

    /**
     * Gets attribute value for specified attribute name of specified chapter.
     *
     * @param chapter the chapter to be retrieved attributes
     * @param name    the attribute name
     * @return the attribute value, or {@literal null} if no attribute found
     * @throws NullPointerException if the chapter or name is null
     */
    public static Object get(@NonNull Chapter chapter, String name) {
        return chapter.getAttributes().get(name);
    }

    /**
     * Gets string value for specified attribute of specified chapter.
     *
     * @param chapter the chapter to be retrieved attributes
     * @param name    the attribute name
     * @return the string value, or {@literal null} if no attribute found
     * @throws NullPointerException if the chapter or name is null
     * @throws ClassCastException   if the attribute value is not a string
     */
    public static String getString(@NonNull Chapter chapter, String name) {
        return chapter.getAttributes().get(name, null);
    }

    /**
     * Gets string values for specified attribute of specified chapter.
     *
     * @param chapter the chapter to be retrieved attributes
     * @param name    the attribute name
     * @return the string values, or {@literal null} if no attribute found
     * @throws NullPointerException if the chapter or name is null
     * @throws ClassCastException   if the attribute value is not a string
     */
    public static Collection<? extends CharSequence> getValues(@NonNull Chapter chapter, String name) {
        val value = getString(chapter, name);
        return isNotEmpty(value) ? Arrays.asList(value.split(VALUES_SEPARATOR)) : null;
    }

    /**
     * Sets specified value for specified attribute name of specified chapter.
     *
     * @param chapter the chapter to be set attributes
     * @param name    the attribute name
     * @param value   the new attribute value
     * @throws NullPointerException if the chapter, name or value is null
     */
    public static void set(@NonNull Chapter chapter, String name, Object value) {
        chapter.getAttributes().set(name, value);
    }

    /**
     * Sets specified values for specified attribute name of specified chapter.
     *
     * @param chapter the chapter to be set attributes
     * @param name    the attribute name
     * @param values  the new attribute values
     * @throws NullPointerException if the chapter, name or values is null
     */
    public static void set(@NonNull Chapter chapter, String name, @NonNull Collection<? extends CharSequence> values) {
        set(chapter, name, join(VALUES_SEPARATOR, values.iterator()));
    }

    private static void mapBuiltins() {
        Properties map;
        try {
            map = propertiesFor("!jem/attributes.properties");
        } catch (IOException e) {
            Log.e(TAG, "cannot load attribtue type mapping", e);
            return;
        }
        if (map == null) {
            return;
        }
        update(typeMappings, map);
    }

    static {
        mapBuiltins();
    }
}
