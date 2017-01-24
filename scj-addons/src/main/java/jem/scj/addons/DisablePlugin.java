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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import jem.scj.app.AppConfig;
import jem.scj.app.SCI;
import jem.scj.app.SCJPlugin;
import lombok.val;
import pw.phylame.commons.io.IOUtils;
import pw.phylame.commons.log.Log;
import pw.phylame.commons.util.CollectionUtils;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Command;
import pw.phylame.qaf.cli.Initializer;
import pw.phylame.qaf.core.Metadata;

public class DisablePlugin extends SCJPlugin implements Initializer, Command {
    private static final String TAG = DisablePlugin.class.getSimpleName();

    private static final String OPTION = "D";

    private SCI sci = getSci();
    private AppConfig config = getConfig();

    public DisablePlugin() {
        super(new Metadata("c240736a-52c6-41ee-afce-9c505c74015a", "Disable Plugin", "1.0", "PW"));
    }

    @Override
    public void init() {
        sci.addOption(Option.builder(OPTION)
                .longOpt("disable-plugin")
                .hasArg()
                .argName(M.tr("disablePlugin.argName"))
                .desc(M.tr("disablePlugin.tip"))
                .build(),
                this);
    }

    @Override
    public int execute(CLIDelegate delegate) {
        val paths = (String[]) sci.getContext().get(OPTION);
        if (paths != null && paths.length > 0) {
            val set = new LinkedHashSet<String>();
            try (val reader = new FileReader(config.getBlacklist())) {
                CollectionUtils.extend(set, IOUtils.linesOf(reader, true));
                Collections.addAll(set, paths);
            } catch (FileNotFoundException e) {
                Log.d(TAG, "not found blacklist file: %s", config.getBlacklist());
            } catch (IOException e) {
                Log.d(TAG, e);
            }
            if (!set.isEmpty()) {
                try (val writer = new FileWriter(config.getBlacklist())) {
                    for (val path : set) {
                        writer.append(path).append('\n');
                    }
                } catch (IOException e) {
                    Log.d(TAG, e);
                }
            }
        }
        return 0;
    }

    @Override
    public void perform(CLIDelegate delegate, CommandLine cmd) {
        sci.getContext().put(OPTION, cmd.getOptionValues(OPTION));
    }

}
