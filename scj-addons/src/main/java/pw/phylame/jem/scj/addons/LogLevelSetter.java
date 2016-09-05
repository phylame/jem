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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import lombok.val;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Initializer;
import pw.phylame.ycl.log.Level;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.StringUtils;

public class LogLevelSetter extends AbstractPlugin implements Initializer {
    private static final String OPTION = "L";
    private static final String OPTION_LONG = "log-level";
    private static final String CONFIG_KEY = "app.log.level";

    public LogLevelSetter() {
        super(new Metadata("367928f7-c47e-43bc-8c10-adcf51301c44", "Log Level Setter", "1.0", "PW"));
    }

    @Override
    public void init() {
        setByConfig();
        addLogOption();
    }

    @Override
    public void perform(CLIDelegate delegate, CommandLine cmd) {
        val level = Level.forName(cmd.getOptionValue(OPTION), Log.getLevel());
        sci.getContext().put(OPTION, level);
        Log.setLevel(level);
    }

    private void addLogOption() {
        String level = config.rawFor(CONFIG_KEY);
        if (level == null) {
            level = Log.getLevel().getName();
        }
        sci.addOption(Option.builder(OPTION)
                .longOpt(OPTION_LONG)
                .hasArg()
                .argName(Messages.tr("logSetter.argName"))
                .desc(Messages.tr("help.setLogLevel", makeLevelList(), level))
                .build(),
                this);
    }

    public static String makeLevelList() {
        val b = new StringBuilder(36);
        int i = 1, end = Level.values().length;
        for (val level : Level.values()) {
            b.append('"').append(level.getName()).append('"');
            if (i++ != end) {
                b.append(", ");
            }
        }
        return b.toString();
    }

    private void setByConfig() {
        val level = config.rawFor(CONFIG_KEY);
        if (StringUtils.isNotEmpty(level)) {
            Log.setLevel(Level.forName(level, Level.DEFAULT_LEVEL));
        }
    }
}
