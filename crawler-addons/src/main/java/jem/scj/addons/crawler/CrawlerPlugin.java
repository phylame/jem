/*
 * Copyright 2017 Peng Wan <phylame@163.com>
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

package jem.scj.addons.crawler;

import lombok.val;
import pw.phylame.qaf.core.Plugin;

import java.util.LinkedHashMap;
import java.util.Map;

public class CrawlerPlugin implements Plugin {
    @Override
    public String getId() {
        return "541a97cb-70fb-4d32-8b3f-7f78eecebba4";
    }

    @Override
    public Map<String, Object> getMeta() {
        val map = new LinkedHashMap<String, Object>();
        map.put("id", getId());
        map.put("name", "Crawler Plugin");
        map.put("version", "1.0");
        map.put("vendor", "PW");
        return map;
    }

    @Override
    public void init() {

    }

    @Override
    public void destroy() {

    }
}
