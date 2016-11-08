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

package pw.phylame.jem.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.util.Variants;
import pw.phylame.jem.util.VariantMap.Validator;
import pw.phylame.jem.util.flob.Flob;
import pw.phylame.jem.util.text.Text;
import pw.phylame.ycl.util.Exceptions;
import pw.phylame.ycl.util.StringUtils;

/**
 * Declares name of attributes supported by Jem for chapter and book.
 *
 * @since 2.1.0
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

    public static final String MULTI_SEPARATOR = ";";

    private static final Map<String, String> attributeTypes = new ConcurrentHashMap<>();

    static {
        attributeTypes.put(COVER, Variants.FLOB);
        attributeTypes.put(INTRO, Variants.TEXT);
        attributeTypes.put(WORDS, Variants.INTEGER);
        attributeTypes.put(DATE, Variants.DATETIME);
        attributeTypes.put(LANGUAGE, Variants.LOCALE);
        attributeTypes.put(PAGES, Variants.INTEGER);
        attributeTypes.put(PRICE, Variants.REAL);
        attributeTypes.put(PUBDATE, Variants.DATETIME);
    }

    public static class AttributeValidator implements Validator {
        @Override
        public void validate(String name, Object value) throws RuntimeException {
            val attributeType = typeOf(name);
            if (attributeType == null) { // unknown attribute name, don't validate
                return;
            }
            val valueType = Variants.typeOf(value);
            if (!attributeType.equals(valueType)) {
                throw Exceptions.forIllegalArgument("attribute '%s' must be '%s'", name, attributeType);
            }
        }
    }

    public static void mapType(String name, String type) {
        attributeTypes.put(name, type);
    }

    /**
     * Returns type of specified attribute name.
     *
     * @param name
     *            name of attribute or {@literal null} if unknown
     * @return the type string
     */
    public static String typeOf(String name) {
        return attributeTypes.get(name);
    }

    public static List<String> getValues(@NonNull Chapter chapter, String name) {
        val value = chapter.getAttributes().get(name, StringUtils.EMPTY_TEXT);
        return !value.isEmpty() ? Arrays.asList(value.split(MULTI_SEPARATOR)) : Collections.<String>emptyList();
    }

    public static void setValues(@NonNull Chapter chapter, String name, Collection<String> values) {
        chapter.getAttributes().set(name, StringUtils.join(MULTI_SEPARATOR, values));
    }

    public static String getTitle(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(TITLE, StringUtils.EMPTY_TEXT);
    }

    public static void setTitle(@NonNull Chapter chapter, String title) {
        chapter.getAttributes().set(TITLE, title);
    }

    public static Flob getCover(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(COVER, Flob.class, null);
    }

    public static void setCover(@NonNull Chapter chapter, Flob cover) {
        chapter.getAttributes().set(COVER, cover);
    }

    public static Text getIntro(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(INTRO, Text.class, null);
    }

    public static void setIntro(@NonNull Chapter chapter, Text intro) {
        chapter.getAttributes().set(INTRO, intro);
    }

    public static Integer getWords(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(WORDS, Integer.class, 0);
    }

    public static void setWords(@NonNull Chapter chapter, int words) {
        chapter.getAttributes().set(WORDS, words);
    }

    public static String getAuthor(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(AUTHOR, StringUtils.EMPTY_TEXT);
    }

    public static void setAuthor(@NonNull Chapter chapter, String author) {
        chapter.getAttributes().set(AUTHOR, author);
    }

    public static Date getDate(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(DATE, Date.class, null);
    }

    public static void setDate(@NonNull Chapter chapter, Date date) {
        chapter.getAttributes().set(DATE, date);
    }

    public static Date getPubdate(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(PUBDATE, Date.class, null);
    }

    public static void setPubdate(@NonNull Chapter chapter, Date pubdate) {
        chapter.getAttributes().set(PUBDATE, pubdate);
    }

    public static String getGenre(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(GENRE, StringUtils.EMPTY_TEXT);
    }

    public static void setGenre(@NonNull Chapter chapter, String genre) {
        chapter.getAttributes().set(GENRE, genre);
    }

    public static Locale getLanguage(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(LANGUAGE, Locale.class, null);
    }

    public static void setLanguage(@NonNull Chapter chapter, Locale language) {
        chapter.getAttributes().set(LANGUAGE, language);
    }

    public static String getPublisher(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(PUBLISHER, StringUtils.EMPTY_TEXT);
    }

    public static void setPublisher(@NonNull Chapter chapter, String publisher) {
        chapter.getAttributes().set(PUBLISHER, publisher);
    }

    public static String getRights(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(RIGHTS, StringUtils.EMPTY_TEXT);
    }

    public static void setRights(@NonNull Chapter chapter, String rights) {
        chapter.getAttributes().set(RIGHTS, rights);
    }

    public static String getState(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(STATE, StringUtils.EMPTY_TEXT);
    }

    public static void setState(@NonNull Chapter chapter, String state) {
        chapter.getAttributes().set(STATE, state);
    }

    public static String getKeywords(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(KEYWORDS, StringUtils.EMPTY_TEXT);
    }

    public static void setKeywords(@NonNull Chapter chapter, String keywords) {
        chapter.getAttributes().set(KEYWORDS, keywords);
    }

    public static void setKeywords(@NonNull Chapter chapter, @NonNull Collection<String> keywords) {
        setValues(chapter, KEYWORDS, keywords);
    }

    public static String getVendor(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(VENDOR, StringUtils.EMPTY_TEXT);
    }

    public static void setVendor(@NonNull Chapter chapter, String vendor) {
        chapter.getAttributes().set(VENDOR, vendor);
    }

    public static String getISBN(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(ISBN, StringUtils.EMPTY_TEXT);
    }

    public static void setISBN(@NonNull Chapter chapter, String isbn) {
        chapter.getAttributes().set(ISBN, isbn);
    }

    public static String getProtagonists(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(PROTAGONISTS, StringUtils.EMPTY_TEXT);
    }

    public static void setProtagonists(@NonNull Chapter chapter, String protagonists) {
        chapter.getAttributes().set(PROTAGONISTS, protagonists);
    }

    public static void setProtagonists(@NonNull Chapter chapter, @NonNull Collection<String> protagonists) {
        setValues(chapter, PROTAGONISTS, protagonists);
    }

    public static String getTranslators(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(TRANSLATORS, StringUtils.EMPTY_TEXT);
    }

    public static void setTranslators(@NonNull Chapter chapter, String translators) {
        chapter.getAttributes().set(TRANSLATORS, translators);
    }

    public static void setTranslators(@NonNull Chapter chapter, @NonNull Collection<String> translators) {
        setValues(chapter, TRANSLATORS, translators);
    }
}
