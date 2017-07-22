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

package jem.epm.impl;

import jem.epm.EpmFactory;
import jem.epm.Maker;
import jem.epm.Parser;

import java.util.Set;

public abstract class AbstractFactory implements EpmFactory {
    @Override
    public Set<String> getNames() {
        return null;
    }

    @Override
    public Parser getParser() {
        return null;
    }

    @Override
    public Maker getMaker() {
        return null;
    }

    @Override
    public String toString() {
        return getName();
    }
}
