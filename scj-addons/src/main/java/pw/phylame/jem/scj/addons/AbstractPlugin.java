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

import java.util.Map;

import pw.phylame.jem.scj.app.AppConfig;
import pw.phylame.jem.scj.app.SCI;
import pw.phylame.qaf.core.App;
import pw.phylame.qaf.core.Plugin;

public abstract class AbstractPlugin implements Plugin {
    private final Metadata metadata;

    protected final App app = App.INSTANCE;

    protected final SCI sci = SCI.INSTANCE;

    protected final AppConfig config = AppConfig.INSTANCE;

    protected AbstractPlugin(Metadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public final String getId() {
        return metadata.getId();
    }

    @Override
    public final Map<String, Object> getMeta() {
        return metadata.toMap();
    }

    @Override
    public void destroy() {

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [id=" + getId() + ", meta=" + getMeta() + "]";
    }
}
