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
    private Map<CharSequence, Object> map;

    private Validator validator = null;

    public VariantMap() {
        this(new HashMap<CharSequence, Object>(), null);
    }

    public VariantMap(@NonNull Map<CharSequence, Object> map, Validator validator) {
        this.map = map;
        this.validator = validator;
    }

    public void put(@NonNull CharSequence key, @NonNull Object value) {
        if (validator != null) {
            validator.validate(key, value);
        }
        map.put(key, value);
    }

    public void update(@NonNull Map<CharSequence, Object> map) {
        for (val e : map.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    public void update(@NonNull VariantMap rhs) {
        update(rhs.map);
    }

    public boolean contains(CharSequence key) {
        return map.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(CharSequence key, T defaultValue, Class<T> type) {
        Object v = map.get(key);
        return v != null && type.isInstance(v) ? (T) v : defaultValue;
    }

    public Object get(CharSequence key, Object defaultValue) {
        Object v = map.get(key);
        return v != null ? v : defaultValue;
    }

    public String get(CharSequence key, String defaultString) {
        Object v = map.get(key);
        return (v != null) ? v.toString() : defaultString;
    }

    public Object remove(CharSequence key) {
        return map.remove(key);
    }

    public void clear() {
        map.clear();
    }

    public int size() {
        return map.size();
    }

    public CharSequence[] keys() {
        return map.keySet().toArray(new CharSequence[map.size()]);
    }

    public Set<Map.Entry<CharSequence, Object>> entries() {
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
        val dump = new VariantMap(new HashMap<CharSequence, Object>(), validator);
        dump.update(this.map);
        return dump;
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
