/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License;public static final   String  Version 2.0 (the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing;public static final   String  software
 * distributed under the License is distributed on an "AS IS" BASIS;public static final   String 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND;public static final   String  either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pw.phylame.jem.core;

import lombok.NonNull;
import pw.phylame.jem.util.Flob;
import pw.phylame.jem.util.Text;

import java.util.Date;
import java.util.Locale;

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
    public static final String PROTAGONIST = "protagonist";
    public static final String PUBDATE = "pubdate";
    public static final String PUBLISHER = "publisher";
    public static final String RIGHTS = "rights";
    public static final String SERIES = "series";
    public static final String STATE = "state";
    public static final String TITLE = "title";
    public static final String TRANSLATOR = "translator";
    public static final String VENDOR = "vendor";
    public static final String WORDS = "words";

    public static String getTitle(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(TITLE, "");
    }

    public static void setTitle(@NonNull Chapter chapter, String title) {
        chapter.getAttributes().put(TITLE, title);
    }

    public static Flob getCover(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(COVER, null, Flob.class);
    }

    public static void setCover(@NonNull Chapter chapter, Flob cover) {
        chapter.getAttributes().put(COVER, cover);
    }

    public static Text getIntro(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(INTRO, null, Text.class);
    }

    public static void setIntro(@NonNull Chapter chapter, Text intro) {
        chapter.getAttributes().put(INTRO, intro);
    }

    public static Integer getWords(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(WORDS, 0, Integer.class);
    }

    public static void setWords(@NonNull Chapter chapter, int words) {
        chapter.getAttributes().put(WORDS, words);
    }

    public static String getAuthor(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(AUTHOR, "");
    }

    public static void setAuthor(@NonNull Chapter chapter, String author) {
        chapter.getAttributes().put(AUTHOR, author);
    }

    public static Date getDate(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(DATE, null, Date.class);
    }

    public static void setDate(@NonNull Chapter chapter, Date date) {
        chapter.getAttributes().put(DATE, date);
    }

    public static Date getPubdate(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(PUBDATE, null, Date.class);
    }

    public static void setPubdate(@NonNull Chapter chapter, Date pubdate) {
        chapter.getAttributes().put(PUBDATE, pubdate);
    }

    public static String getGenre(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(GENRE, "");
    }

    public static void setGenre(@NonNull Chapter chapter, String genre) {
        chapter.getAttributes().put(GENRE, genre);
    }

    public static Locale getLanguage(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(LANGUAGE, null, Locale.class);
    }

    public static void setLanguage(@NonNull Chapter chapter, Locale language) {
        chapter.getAttributes().put(LANGUAGE, language);
    }

    public static String getPublisher(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(PUBLISHER, "");
    }

    public static void setPublisher(@NonNull Chapter chapter, String publisher) {
        chapter.getAttributes().put(PUBLISHER, publisher);
    }

    public static String getRights(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(RIGHTS, "");
    }

    public static void setRights(@NonNull Chapter chapter, String rights) {
        chapter.getAttributes().put(RIGHTS, rights);
    }

    public static String getState(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(STATE, "");
    }

    public static void setState(@NonNull Chapter chapter, String state) {
        chapter.getAttributes().put(STATE, state);
    }

    public static String getKeywords(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(KEYWORDS, "");
    }

    public static void setKeywords(@NonNull Chapter chapter, String subject) {
        chapter.getAttributes().put(KEYWORDS, subject);
    }

    public static String getVendor(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(VENDOR, "");
    }

    public static void setVendor(@NonNull Chapter chapter, String vendor) {
        chapter.getAttributes().put(VENDOR, vendor);
    }

    public static String getISBN(@NonNull Chapter chapter) {
        return chapter.getAttributes().get(ISBN, "");
    }

    public static void setISBN(@NonNull Chapter chapter, String isbn) {
        chapter.getAttributes().put(ISBN, isbn);
    }
}
