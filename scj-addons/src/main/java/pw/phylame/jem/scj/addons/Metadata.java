/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
 *
 * This file is part of SCJ.
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

package pw.phylame.jem.scj.addons;

import lombok.NonNull;
import lombok.Value;
import lombok.val;

import java.util.LinkedHashMap;
import java.util.Map;

@Value
public class Metadata {
    @NonNull
    private String id;
    @NonNull
    private String name;
    @NonNull
    private String version;
    @NonNull
    private String vendor;

    public Map<String, Object> toMap() {
        val map = new LinkedHashMap<String, Object>();
        map.put("id", id);
        map.put("name", name);
        map.put("version", version);
        map.put("vendor", vendor);
        return map;
    }
}
