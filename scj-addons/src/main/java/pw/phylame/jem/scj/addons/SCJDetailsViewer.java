package pw.phylame.jem.scj.addons;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.cli.Option;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import lombok.val;
import pw.phylame.jem.scj.app.AppConfig;
import pw.phylame.jem.scj.app.AppKt;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Command;
import pw.phylame.qaf.cli.PropertiesFetcher;
import pw.phylame.qaf.core.Plugin;
import pw.phylame.ycl.log.Level;

public class SCJDetailsViewer extends AbstractPlugin {

    public SCJDetailsViewer() {
        super(new Metadata("45fd7ef3-aa4a-40a9-bf79-416fbc44aeab", "SCJ Details Viewer", "1.0", "PW"));
    }

    @Override
    public void init() {
        sci.addOption(new Option("V", "view-context", false, AddonsMessages.tr("help.listContext")), new Command() {
            @Override
            public int execute(CLIDelegate arg0) {
                val ctx = sci.getContext();
                if (ctx.isEmpty()) {
                    app.echo(AddonsMessages.tr("listContext.emptyContext"));
                } else {
                    for (val e : ctx.entrySet()) {
                        System.out.printf("%s[%s]=%s\n", e.getKey(), e.getValue().getClass().getSimpleName(),
                                e.getValue());
                    }
                }
                return 0;
            }
        });
        sci.addOption(new Option("U", "list-plugins", false, AddonsMessages.tr("help.listPlugins")), new Command() {
            @Override
            public int execute(CLIDelegate arg0) {
                val plugins = app.getPlugins().values();
                System.out.println(AddonsMessages.tr("listPlugins.tip", plugins.size()));
                for (val plugin : plugins) {
                    printPlugin(plugin);
                }
                return 0;
            }
        });
        sci.addOption(new Option("C", "list-config", false, AddonsMessages.tr("help.listConfig")), new Command() {
            @Override
            public int execute(CLIDelegate arg0) {
                AppConfig.INSTANCE.forEach(new Function1<Map.Entry<String, String>, Unit>() {
                    @Override
                    public Unit invoke(Entry<String, String> e) {
                        System.out.println(e.getKey() + '=' + e.getValue());
                        return null;
                    }
                });
                return 0;
            }
        });
        sci.addOption(Option.builder("S").argName(app.tr("help.kvName")).numberOfArgs(2).valueSeparator()
                .desc(AddonsMessages.tr("help.setConfig")).build(), new ConfigSetter());
    }

    private void printPlugin(Plugin plugin) {
        val pattern = "%" + AppConfig.INSTANCE.get("app.plugin.nameWidth", 8, Integer.class) + "s: %s";
        String text = formatItem(pattern, "id", plugin.getId());
        int width = text.length();
        System.out.println(text);
        text = formatItem(pattern, "path", plugin.getClass().getName());
        width = Math.max(width, text.length());
        System.out.println(text);
        for (val e : plugin.getMeta().entrySet()) {
            text = formatItem(pattern, e.getKey(), e.getValue());
            width = Math.max(width, text.length());
            System.out.println(text);
        }
        System.out.println(multiplyOf("-", width));
    }

    private String formatItem(String pattern, String name, Object value) {
        return String.format(pattern, name, value);
    }

    private String multiplyOf(String str, int count) {
        val b = new StringBuffer();
        for (int i = 0; i < count; i++) {
            b.append(str);
        }
        return b.toString();
    }

    private class ConfigSetter extends PropertiesFetcher implements Command {
        private ConfigSetter() {
            super("S");
        }

        @Override
        public int execute(CLIDelegate delegate) {
            val prop = (Properties) delegate.getContext().get("S");
            val cfg = AppConfig.INSTANCE;
            for (val e : prop.entrySet()) {
                val key = e.getKey().toString();
                val value = e.getValue().toString();
                if (key.equals("app.debug.level")) {
                    if (!AppKt.checkDebugLevel(value)) {
                        continue;
                    }
                } else if (key.equals("app.log.level")) {
                    if (Level.forName(value, null) == null) {
                        app.error(AddonsMessages.tr("logSetter.invalidLevel", value, LogLevelSetter.makeLevelList()));
                        continue;
                    }
                }
                cfg.set(key, value, String.class);
            }
            return 0;
        }
    }
}
