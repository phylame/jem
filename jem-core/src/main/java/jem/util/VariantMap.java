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

package jem.util;

import jclp.function.EntryToPair;
import jclp.value.Pair;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static jclp.util.CollectionUtils.map;
import static jclp.util.StringUtils.isEmpty;
import static jclp.util.Validate.requireNotEmpty;
import static jclp.util.Validate.requireNotNull;

public class VariantMap implements Iterable<Pair<String, Object>>, Cloneable {
    private HashMap<String, Object> values = new HashMap<>();

    private Validator validator;

    public VariantMap() {
        this(null);
    }

    public VariantMap(Validator validator) {
        this.validator = validator;
    }

    public void set(String name, Object value) {
        requireNotEmpty(name, "Name of attribute cannot be empty");
        requireNotNull(value, "Value of attribute cannot be null");
        if (validator != null) {
            validator.validate(name, value);
        }
        values.put(name, value);
    }

    public void update(@NonNull VariantMap others) {
        update(others.values);
    }

    public void update(@NonNull Map<String, Object> values) {
        for (val entry : values.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
    }

    public boolean contains(String name) {
        return !isEmpty(name) && values.containsKey(name);
    }

    public Object get(String name) {
        return isEmpty(name) ? null : values.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String name, T fallback) {
        if (isEmpty(name)) {
            return fallback;
        }
        val value = values.get(name);
        return value != null ? (T) value : fallback;
    }

    public int size() {
        return values.size();
    }

    @Override
    public Iterator<Pair<String, Object>> iterator() {
        return map(values.entrySet().iterator(), new EntryToPair<String, Object>());
    }

    public Object remove(String name) {
        return isEmpty(name) ? null : values.remove(name);
    }

    public void clear() {
        values.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    @SneakyThrows(CloneNotSupportedException.class)
    public VariantMap clone() {
        val copy = (VariantMap) super.clone();
        copy.values = (HashMap<String, Object>) values.clone();
        copy.validator = validator;
        return copy;
    }

    @Override
    public String toString() {
        return values.toString();
    }

    public interface Validator {
        void validate(String name, Object value) throws IllegalArgumentException;
    }
}
