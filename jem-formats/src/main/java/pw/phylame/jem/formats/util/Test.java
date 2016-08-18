/*
 * Copyright 2016 Peng Wan <phylame@163.com>
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

package pw.phylame.jem.formats.util;

import lombok.val;
import pw.phylame.jem.core.Attributes;
import pw.phylame.jem.core.Book;
import pw.phylame.jem.epm.Helper;
import pw.phylame.jem.epm.Registry;
import pw.phylame.jem.formats.pmab.PmabOutConfig;
import pw.phylame.jem.util.JemException;
import pw.phylame.jem.util.flob.Flobs;
import pw.phylame.jem.util.text.Text;
import pw.phylame.jem.util.text.Texts;
import pw.phylame.ycl.util.MiscUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class Test {
    public static void main(String[] args) throws IOException, JemException {
        System.setProperty(Registry.AUTO_LOAD_CUSTOMIZED_KEY, "true");
        System.out.println(Arrays.toString(Registry.supportedMakers()));
        System.out.println(Arrays.toString(Registry.supportedParsers()));

        val path = "F:\\pw\\tmp\\ex3.pmab";
        System.out.println(Helper.readBook(new File(path), "pmab", null).debug());
    }
}
