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

package pw.phylame.jem.crawler.util;

import lombok.NonNull;
import pw.phylame.jem.core.Chapter;
import pw.phylame.jem.crawler.CrawlerProvider;
import pw.phylame.jem.util.text.AbstractText;
import pw.phylame.jem.util.text.Texts;

public class HtmlText extends AbstractText {
    private final String url;
    private final Chapter chapter;
    private final CrawlerProvider crawler;

    public HtmlText(@NonNull String url, @NonNull CrawlerProvider crawler, Chapter chapter) {
        super(Texts.PLAIN);
        this.url = url;
        this.chapter = chapter;
        this.crawler = crawler;
    }

    @Override
    public String getText() {
        return crawler.fetchText(chapter, url);
    }
}
