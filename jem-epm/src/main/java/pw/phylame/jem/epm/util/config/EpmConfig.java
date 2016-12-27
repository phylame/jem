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

package pw.phylame.jem.epm.util.config;

/**
 * Interface indicating a configuration for parser or parser.
 * <p>Sub class should implement the interface and set all field public and with annotation {@code Configured}.</p>
 * <p>Static field with name 'SELF' for finding self in argument map.</p>
 */
public interface EpmConfig {
    String SELF_FIELD_NAME = "SELF";
}
