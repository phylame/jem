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

import jem.epm.EpmManager;
import jem.epm.util.DebugUtils;
import lombok.val;

public class Test {
    public static void main(String[] args) {
        EpmManager.loadImplementors();
        val bookk = DebugUtils.parseFile("E:\\tmp\\掠天记\\掠天记.zip", "pmab", null);
        System.out.println(bookk);
        DebugUtils.makeFile(bookk, "E:/tmp", "epub", null);
    }
}
