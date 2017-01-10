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

import static java.lang.System.out;
import static jem.scj.addons.M.tr;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.cli.Option;

import jem.scj.app.AppConfig;
import jem.scj.app.AppKt;
import jem.scj.app.SCI;
import jem.scj.app.SCJPlugin;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import lombok.val;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Command;
import pw.phylame.qaf.cli.PropertiesFetcher;
import pw.phylame.qaf.core.App;
import pw.phylame.qaf.core.Metadata;
import pw.phylame.qaf.core.Plugin;
import pw.phylame.ycl.log.LogLevel;
import pw.phylame.ycl.util.CollectionUtils;
import pw.phylame.ycl.util.Prediction;
import pw.phylame.ycl.util.Provider;
import pw.phylame.ycl.util.StringUtils;
import pw.phylame.ycl.value.Lazy;

public class AppInspector extends SCJPlugin {

    private SCI sci = getSci();
    private App app = getApp();
    private AppConfig config = getConfig();

    public AppInspector() {
        super(new Metadata("45fd7ef3-aa4a-40a9-bf79-416fbc44aeab", "App Inspector", "1.0", "PW"));
    }

    @Override
    public void init() {
        sci.addOption(new Option("V", "view-context", false, tr("help.listContext")), new Command() {
            @Override
            public int execute(CLIDelegate delegate) {
                val context = sci.getContext();
                if (context.isEmpty()) {
                    app.echo(tr("listContext.emptyContext"));
                } else {
                    for (val e : context.entrySet()) {
                        out.printf("%s[%s]=%s\n", e.getKey(), e.getValue().getClass().getSimpleName(), e.getValue());
                    }
                }
                return 0;
            }
        });
        sci.addOption(new Option("U", "list-plugins", false, tr("help.listPlugins")), new Command() {
            @Override
            public int execute(CLIDelegate delegate) {
                val plugins = app.getPlugins().values();
                out.println(tr("listPlugins.tip", plugins.size()));
                int width = 0;
                List<String> items = new LinkedList<>();
                for (val plugin : plugins) {
                    val b = new StringBuilder();
                    width = Math.max(width, printPlugin(plugin, b));
                    items.add(b.toString());
                }
                out.println(StringUtils.multiplyOf("-", width));
                for (val item : items) {
                    out.print(item);
                    out.println(StringUtils.multiplyOf("-", width));
                }
                return 0;
            }
        });
        sci.addOption(new Option("C", "list-config", false, tr("help.listConfig")), new Command() {
            @Override
            public int execute(CLIDelegate delegate) {
                config.forEach(new Function1<Map.Entry<String, String>, Unit>() {
                    @Override
                    public Unit invoke(Entry<String, String> e) {
                        out.println(e.getKey() + '=' + e.getValue());
                        return null;
                    }
                });
                return 0;
            }
        });
        sci.addOption(Option.builder("S")
                .numberOfArgs(2)
                .argName(app.tr("help.kvName"))
                .valueSeparator()
                .desc(tr("help.setConfig"))
                .build(),
                new ConfigSetter());
    }

    private int printPlugin(Plugin plugin, StringBuilder out) {
        val pattern = "%" + config.get("app.plugin.nameWidth", 8, Integer.class) + "s: %s";
        String text = formatItem(pattern, "id", plugin.getId());
        int width = text.length();
        val nl = '\n';
        out.append(text).append(nl);
        text = formatItem(pattern, "path", plugin.getClass().getName());
        width = Math.max(width, text.length());
        out.append(text).append(nl);
        for (val e : plugin.getMeta().entrySet()) {
            text = formatItem(pattern, e.getKey(), e.getValue());
            width = Math.max(width, text.length());
            out.append(text).append(nl);
        }
        return width;
    }

    private String formatItem(String pattern, String name, Object value) {
        return String.format(pattern, name, value);
    }

    private class ConfigSetter extends PropertiesFetcher implements Command {
        private final Lazy<Map<String, Prediction<String>>> validators = new Lazy<>(
                new Provider<Map<String, Prediction<String>>>() {
                    @Override
                    public Map<String, Prediction<String>> provide() throws Exception {
                        return CollectionUtils.<String, Prediction<String>>mapOf(
                                "app.debug.level", new Prediction<String>() {
                                    @Override
                                    public Boolean apply(String i) {
                                        return AppKt.checkDebugLevel(i);
                                    }
                                },
                                "app.log.level", new Prediction<String>() {
                                    @Override
                                    public Boolean apply(String i) {
                                        if (LogLevel.forName(i, null) == null) {
                                            app.error(tr("logSetter.invalidLevel", i, LogSetter.makeLevelList()));
                                            return false;
                                        }
                                        return true;
                                    }
                                });
                    }
                });

        private ConfigSetter() {
            super("S");
        }

        @Override
        public int execute(CLIDelegate delegate) {
            val prop = (Properties) delegate.getContext().get("S");
            for (val e : prop.entrySet()) {
                val key = e.getKey().toString();
                val value = e.getValue().toString();

                val fun = validators.get().get(key);
                if (fun != null && !fun.apply(value)) {
                    continue;
                }

                config.set(key, value, String.class);
            }
            return 0;
        }
    }
}
