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

import jem.scj.app.AppConfig;
import jem.scj.app.SCI;
import jem.scj.app.SCJPlugin;
import lombok.val;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import jclp.log.Log;
import jclp.log.Level;
import jclp.text.Render;
import jclp.util.StringUtils;
import qaf.cli.CLIDelegate;
import qaf.cli.Initializer;
import qaf.core.Metadata;

import java.util.Arrays;

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
        try {
            val level = Level.valueOf(cmd.getOptionValue(OPTION).toUpperCase());
            sci.getContext().put(OPTION, level);
            Log.setLevel(level);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void addSetOption() {
        String level = config.rawFor(CONFIG_KEY);
        if (StringUtils.isEmpty(level)) {
            level = Log.getLevel().name().toLowerCase();
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
        return StringUtils.join(", ", Arrays.asList(Level.values()), new Render<Level>() {
            @Override
            public String render(Level level) {
                return '\"' + level.name().toLowerCase() + '\"';
            }
        });
    }

    private void setByConfig() {
        val level = config.rawFor(CONFIG_KEY);
        if (StringUtils.isNotEmpty(level)) {
            try {
                Log.setLevel(Level.valueOf(level.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
