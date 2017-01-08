/*
 * Copyright 2014-2017 Peng Wan <phylame@163.com>
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

package jem.scj.addons;

import lombok.val;
import org.apache.commons.cli.Option;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Command;
import pw.phylame.qaf.cli.TypedFetcher;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.Reflections;
import pw.phylame.ycl.util.StringUtils;

import javax.script.ScriptEngine;
import java.io.File;
import java.io.FileReader;

public class ScriptRunner extends AbstractPlugin {
    private static final String TAG = "CSR";

    private static final String OPTION = "R";

    private static final String DEFAULT_ENGINE = "JavaScript";

    public ScriptRunner() {
        super(new Metadata("ff6369df-2b11-4d9d-80e2-1197fc9e088f", "Script Runner", "1.0", "PW"));
    }

    @Override
    public void init() {
        sci.addOption(Option.builder(OPTION)
                .longOpt("run-script")
                .hasArg()
                .argName(M.tr("runScript.file"))
                .desc(M.tr("help.runScript"))
                .build(),
                new RunnerCommand());
    }

    private Object detectScriptEngine() {
        try {
            val clazz = Class.forName("javax.script.ScriptEngineManager");
            String name = config.rawFor("csr.engine.name");
            if (StringUtils.isEmpty(name)) {
                name = DEFAULT_ENGINE;
            }
            val engine = Reflections.i(clazz.newInstance(), "getEngineByName", name);
            if (engine == null) {
                app.error(M.tr("runScript.noSuchEngine", name));
            }
            return engine;
        } catch (Exception e) {
            Log.e(TAG, e);
            app.error(M.tr("runScript.unsupported", System.getProperty("java.version")));
            return null;
        }
    }

    private class RunnerCommand extends TypedFetcher<String> implements Command {

        private RunnerCommand() {
            super(OPTION, String.class, null);
        }

        @Override
        public int execute(CLIDelegate delegate) {
            val file = new File(sci.getContext().get(OPTION).toString());
            if (!file.exists()) {
                app.error(app.tr("error.input.notExists", file.getPath()));
                return -1;
            }
            val result = detectScriptEngine();
            if (result == null) {
                return -1;
            }
            try (val reader = new FileReader(file)) {
                val engine = (ScriptEngine) result;
                engine.put("app", app);
                engine.put("sci", sci);
                engine.put("config", config);
                engine.eval(reader);
            } catch (Exception e) {
                app.error(M.tr("runScript.error"), e);
                return -1;
            }
            return 0;
        }
    }

}
