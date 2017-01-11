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

import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import jem.scj.app.AppConfig;
import jem.scj.app.SCI;
import jem.scj.app.SCJPlugin;
import lombok.val;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Initializer;
import pw.phylame.qaf.core.Metadata;
import pw.phylame.ycl.format.Render;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.log.LogLevel;
import pw.phylame.ycl.util.StringUtils;

public class LogSetter extends SCJPlugin implements Initializer {
    private static final String OPTION = "L";
    private static final String OPTION_LONG = "log-level";
    private static final String CONFIG_KEY = "app.log.level";

    private SCI sci = getSci();
    private AppConfig config = getConfig();

    public LogSetter() {
        super(new Metadata("367928f7-c47e-43bc-8c10-adcf51301c44", "Log Setter", "1.0", "PW"));
    }

    @Override
    public void init() {
        setByConfig();
        addSetOption();
    }

    @Override
    public void perform(CLIDelegate delegate, CommandLine cmd) {
        val level = LogLevel.forName(cmd.getOptionValue(OPTION), Log.getLevel());
        sci.getContext().put(OPTION, level);
        Log.setLevel(level);
    }

    private void addSetOption() {
        String level = config.rawFor(CONFIG_KEY);
        if (StringUtils.isEmpty(level)) {
            level = Log.getLevel().getName();
        }
        sci.addOption(Option.builder(OPTION)
                .longOpt(OPTION_LONG)
                .hasArg()
                .argName(M.tr("logSetter.argName"))
                .desc(M.tr("help.setLogLevel", makeLevelList(), level))
                .build(),
                this);
    }

    public static String makeLevelList() {
        return StringUtils.join(", ", Arrays.asList(LogLevel.values()), new Render<LogLevel>() {
            @Override
            public String render(LogLevel level) {
                return '\"' + level.getName() + '\"';
            }
        });
    }

    private void setByConfig() {
        val level = config.rawFor(CONFIG_KEY);
        if (StringUtils.isNotEmpty(level)) {
            Log.setLevel(LogLevel.forName(level, LogLevel.DEFAULT));
        }
    }
}
