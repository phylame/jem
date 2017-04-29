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

package jem.util;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.val;
import pw.phylame.commons.util.StringUtils;
import pw.phylame.commons.util.Validate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VariantMap implements Cloneable {
    private Map<String, Object> map;

    public interface Validator {
        void validate(String name, Object value) throws RuntimeException;
    }

    @Getter
    @Setter
    private Validator validator = null;

    public VariantMap() {
        this(new HashMap<String, Object>(), null);
    }

    public VariantMap(@NonNull Map<String, Object> map, Validator validator) {
        this.map = map;
        this.validator = validator;
    }

    public void set(String name, @NonNull Object value) {
        Validate.requireNotEmpty(name, "name cannot be empty");
        if (validator != null) {
            validator.validate(name, value);
        }
        map.put(name, value);
    }

    public void update(@NonNull Map<String, Object> map) {
        for (val e : map.entrySet()) {
            set(e.getKey(), e.getValue());
        }
    }

    public void update(@NonNull VariantMap rhs) {
        update(rhs.map);
    }

    public boolean contains(String name) {
        return !StringUtils.isEmpty(name) && map.containsKey(name);
    }

    public Object get(String name) {
        return StringUtils.isEmpty(name) ? null : map.get(name);
    }

    public Object get(String name, Object fallback) {
        if (StringUtils.isEmpty(name)) {
            return fallback;
        }
        val value = map.get(name);
        return value != null ? value : fallback;
    }

    public String get(String name, String fallback) {
        if (StringUtils.isEmpty(name)) {
            return fallback;
        }
        val value = map.get(name);
        return (value != null) ? value.toString() : fallback;
    }

    public <T> T get(String name, @NonNull Class<T> type, T fallback) {
        if (StringUtils.isEmpty(name)) {
            return fallback;
        }
        val value = map.get(name);
        return (value != null && type.isInstance(value)) ? type.cast(value) : fallback;
    }

    public Object remove(String name) {
        return StringUtils.isEmpty(name) ? null : map.remove(name);
    }

    public void clear() {
        map.clear();
    }

    public int size() {
        return map.size();
    }

    public Set<Map.Entry<String, Object>> entries() {
        return map.entrySet();
    }

    public Set<String> names() {
        return map.keySet();
    }

    /**
     * Returns a shallow copy of this {@code VariantMap} instance: the names and values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    @Override
    @SuppressWarnings("unchecked")
    public VariantMap clone() {
        final VariantMap copy;
        try {
            copy = (VariantMap) super.clone();
            copy.map = map.getClass().newInstance();
        } catch (CloneNotSupportedException | InstantiationException | IllegalAccessException e) {
            throw new InternalError();
        }
        copy.map.putAll(map);
        return copy;
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
