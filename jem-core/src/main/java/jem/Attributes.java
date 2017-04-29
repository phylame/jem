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

package jem;

import jem.util.M;
import jem.util.VariantMap.Validator;
import jem.util.Variants;
import jem.util.flob.Flob;
import jem.util.text.Text;
import jem.util.text.Texts;
import lombok.NonNull;
import lombok.val;
import pw.phylame.commons.log.Log;
import pw.phylame.commons.util.Exceptions;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static pw.phylame.commons.util.CollectionUtils.propertiesFor;
import static pw.phylame.commons.util.CollectionUtils.update;
import static pw.phylame.commons.util.StringUtils.EMPTY_TEXT;
import static pw.phylame.commons.util.StringUtils.join;

/**
 * Declares name of standard attributes of book(or chapter) supported by Jem.
 */
public final class Attributes {
    public static final String AUTHOR = "author";
    public static final String BINDING = "binding";
    public static final String COVER = "cover";
    public static final String DATE = "date";
    public static final String GENRE = "genre";
    public static final String INTRO = "intro";
    public static final String ISBN = "isbn";
    public static final String KEYWORDS = "keywords";
    public static final String LANGUAGE = "language";
    public static final String PAGES = "pages";
    public static final String PRICE = "price";
    public static final String PROTAGONISTS = "protagonists";
    public static final String PUBDATE = "pubdate";
    public static final String PUBLISHER = "publisher";
    public static final String RIGHTS = "rights";
    public static final String SERIES = "series";
    public static final String STATE = "state";
    public static final String TITLE = "title";
    public static final String TRANSLATORS = "translators";
    public static final String VENDOR = "vendor";
    public static final String WORDS = "words";

    public static final Validator variantValidator = new Validator() {
        @Override
        public void validate(String name, Object value) throws RuntimeException {
            val type = typeOf(name);
            if (type == null) { // unknown attribute name, don't validate
                return;
            }
            if (!type.equals(Variants.typeOf(value))) {
                throw Exceptions.forIllegalArgument("attribute '%s' must be '%s'", name, type);
            }
        }
    };

    private static final Map<String, String> typeMapping;

    static {
        typeMapping = new ConcurrentHashMap<>();
        try {
            update(typeMapping, propertiesFor("!jem/util/attributes.properties"));
        } catch (IOException e) {
            Log.d("Attributes", e);
        }
    }

    /**
     * Returns supported names of attribute by Jem.
     *
     * @return set of attribute names
     */
    public static Set<String> supportedNames() {
        return typeMapping.keySet();
    }

    /**
     * Gets readable text for attribute name.
     *
     * @param name name of attribute
     * @return the text, or {@literal null} if the name is unknown
     */
    public static String titleOf(@NonNull String name) {
        try {
            return M.tr("attribute." + name);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    /**
     * Maps specified attribute name with specified variant type.
     *
     * @param name name of attribute
     * @param type name of type
     */
    public static void mapType(@NonNull String name, String type) {
        typeMapping.put(name, Variants.ensureRegistered(type));
    }

    /**
     * Gets the type of specified attribute name.
     *
     * @param name name of attribute or {@literal null} if unknown
     * @return the type string
     */
    public static String typeOf(String name) {
        return typeMapping.get(name);
    }

    /**
     * Returns default value for specified attribute.
     *
     * @param name name of attribute
     * @return the value, or {@literal null} if the name is unknown
     */
    public static Object defaultOf(String name) {
        val type = typeOf(name);
        return type == null ? null : Variants.defaultOf(type);
    }

    public static final String VALUES_SEPARATOR = ";";

    public static List<String> getValues(Chapter chapter, String name) {
        val value = chapter.getAttributes().get(name, EMPTY_TEXT);
        return !value.isEmpty() ? Arrays.asList(value.split(VALUES_SEPARATOR)) : Collections.<String>emptyList();
    }

    public static void setValues(Chapter chapter, String name, Collection<?> values) {
        chapter.getAttributes().set(name, join(VALUES_SEPARATOR, values));
    }

    public static String getTitle(Chapter chapter) {
        return chapter.getAttributes().get(TITLE, EMPTY_TEXT);
    }

    public static void setTitle(Chapter chapter, String title) {
        chapter.getAttributes().set(TITLE, title);
    }

    public static Flob getCover(Chapter chapter) {
        return chapter.getAttributes().get(COVER, Flob.class, null);
    }

    public static void setCover(Chapter chapter, Flob cover) {
        chapter.getAttributes().set(COVER, cover);
    }

    public static Text getIntro(Chapter chapter) {
        return chapter.getAttributes().get(INTRO, Text.class, null);
    }

    public static void setIntro(Chapter chapter, Text intro) {
        chapter.getAttributes().set(INTRO, intro);
    }

    public static void setIntro(Chapter chapter, String intro) {
        chapter.getAttributes().set(INTRO, Texts.forString(intro, Texts.PLAIN));
    }

    public static String getWords(Chapter chapter) {
        return chapter.getAttributes().get(WORDS, String.class, null);
    }

    public static void setWords(Chapter chapter, int words) {
        chapter.getAttributes().set(WORDS, String.valueOf(words));
    }

    public static void setWords(Chapter chapter, String words) {
        chapter.getAttributes().set(WORDS, words);
    }

    public static String getAuthor(Chapter chapter) {
        return chapter.getAttributes().get(AUTHOR, EMPTY_TEXT);
    }

    public static void setAuthor(Chapter chapter, String author) {
        chapter.getAttributes().set(AUTHOR, author);
    }

    public static void setAuthors(Chapter chapter, Collection<?> authors) {
        setValues(chapter, AUTHOR, authors);
    }

    public static Date getDate(Chapter chapter) {
        return chapter.getAttributes().get(DATE, Date.class, null);
    }

    public static void setDate(Chapter chapter, Date date) {
        chapter.getAttributes().set(DATE, date);
    }

    public static Date getPubdate(Chapter chapter) {
        return chapter.getAttributes().get(PUBDATE, Date.class, null);
    }

    public static void setPubdate(Chapter chapter, Date pubdate) {
        chapter.getAttributes().set(PUBDATE, pubdate);
    }

    public static String getGenre(Chapter chapter) {
        return chapter.getAttributes().get(GENRE, EMPTY_TEXT);
    }

    public static void setGenre(Chapter chapter, String genre) {
        chapter.getAttributes().set(GENRE, genre);
    }

    public static Locale getLanguage(Chapter chapter) {
        return chapter.getAttributes().get(LANGUAGE, Locale.class, null);
    }

    public static void setLanguage(Chapter chapter, Locale language) {
        chapter.getAttributes().set(LANGUAGE, language);
    }

    public static String getPublisher(Chapter chapter) {
        return chapter.getAttributes().get(PUBLISHER, EMPTY_TEXT);
    }

    public static void setPublisher(Chapter chapter, String publisher) {
        chapter.getAttributes().set(PUBLISHER, publisher);
    }

    public static String getRights(Chapter chapter) {
        return chapter.getAttributes().get(RIGHTS, EMPTY_TEXT);
    }

    public static void setRights(Chapter chapter, String rights) {
        chapter.getAttributes().set(RIGHTS, rights);
    }

    public static String getState(Chapter chapter) {
        return chapter.getAttributes().get(STATE, EMPTY_TEXT);
    }

    public static void setState(Chapter chapter, String state) {
        chapter.getAttributes().set(STATE, state);
    }

    public static String getKeywords(Chapter chapter) {
        return chapter.getAttributes().get(KEYWORDS, EMPTY_TEXT);
    }

    public static void setKeywords(Chapter chapter, String keywords) {
        chapter.getAttributes().set(KEYWORDS, keywords);
    }

    public static void setKeywords(Chapter chapter, Collection<?> keywords) {
        setValues(chapter, KEYWORDS, keywords);
    }

    public static String getVendor(Chapter chapter) {
        return chapter.getAttributes().get(VENDOR, EMPTY_TEXT);
    }

    public static void setVendor(Chapter chapter, String vendor) {
        chapter.getAttributes().set(VENDOR, vendor);
    }

    public static String getISBN(Chapter chapter) {
        return chapter.getAttributes().get(ISBN, EMPTY_TEXT);
    }

    public static void setISBN(Chapter chapter, String isbn) {
        chapter.getAttributes().set(ISBN, isbn);
    }

    public static String getProtagonists(Chapter chapter) {
        return chapter.getAttributes().get(PROTAGONISTS, EMPTY_TEXT);
    }

    public static void setProtagonists(Chapter chapter, String protagonists) {
        chapter.getAttributes().set(PROTAGONISTS, protagonists);
    }

    public static void setProtagonists(Chapter chapter, Collection<?> protagonists) {
        setValues(chapter, PROTAGONISTS, protagonists);
    }

    public static String getTranslators(Chapter chapter) {
        return chapter.getAttributes().get(TRANSLATORS, EMPTY_TEXT);
    }

    public static void setTranslators(Chapter chapter, String translators) {
        chapter.getAttributes().set(TRANSLATORS, translators);
    }

    public static void setTranslators(Chapter chapter, Collection<?> translators) {
        setValues(chapter, TRANSLATORS, translators);
    }
}
