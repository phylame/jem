/*
 * Copyright 2014-2016 Peng Wan <phylame@163.com>
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

package pw.phylame.jem.util;

import lombok.NonNull;
import lombok.val;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VariantMap implements Cloneable {
    private Map<String, Object> map;

    private Validator validator = null;

    public VariantMap() {
        this(new HashMap<String, Object>(), null);
    }

    public VariantMap(@NonNull Map<String, Object> map, Validator validator) {
        this.map = map;
        this.validator = validator;
    }

    public void put(@NonNull String key, @NonNull Object value) {
        if (validator != null) {
            validator.validate(key, value);
        }
        map.put(key, value);
    }

    public void update(@NonNull Map<String, Object> map) {
        for (val e : map.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    public void update(@NonNull VariantMap rhs) {
        update(rhs.map);
    }

    public boolean contains(String key) {
        return map.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T fallback, Class<T> type) {
        Object v = map.get(key);
        return v != null && type.isInstance(v) ? (T) v : fallback;
    }

    public Object get(String key, Object fallback) {
        Object v = map.get(key);
        return v != null ? v : fallback;
    }

    public Object get(String key) {
        return map.get(key);
    }

    public String get(String key, String fallback) {
        Object v = map.get(key);
        return (v != null) ? v.toString() : fallback;
    }

    public Object remove(String key) {
        return map.remove(key);
    }

    public void clear() {
        map.clear();
    }

    public int size() {
        return map.size();
    }

    public String[] keys() {
        return map.keySet().toArray(new String[map.size()]);
    }

    public Set<Map.Entry<String, Object>> entries() {
        return map.entrySet();
    }

    /**
     * Returns a shallow copy of this <code>VariantMap</code> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    @Override
    public VariantMap clone() {
        val dump = new VariantMap(new HashMap<String, Object>(), validator);
        dump.update(this.map);
        return dump;
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
