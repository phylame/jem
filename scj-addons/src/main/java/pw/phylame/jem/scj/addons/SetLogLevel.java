package pw.phylame.jem.scj.addons;

import lombok.val;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import pw.phylame.jem.scj.app.AppConfig;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Initializer;
import pw.phylame.ycl.log.Level;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.StringUtils;

public class SetLogLevel extends AbstractPlugin implements Initializer {
    private static final String TAG = "SLL";
    private static final String OPTION = "L";
    private static final String OPTION_LONG = "log-level";
    private static final String CONFIG_KEY = "app.log.level";

    public SetLogLevel() {
        super(new Metadata("Set Log Level", "1.0", "PW"));
    }

    @Override
    public void init() {
        setByConfig();
        addLogOption();
    }

    @Override
    public void perform(CLIDelegate delegate, CommandLine cmd) {
        val level = Level.forName(cmd.getOptionValue(OPTION), Level.DEFAULT_LEVEL);
        sci.getContext().put("logLevel", level);
        Log.setLevel(level);
    }

    private void addLogOption() {
        val b = new StringBuilder(36);
        int i = 1, end = Level.values().length;
        for (val level : Level.values()) {
            b.append('"').append(level.getName()).append('"');
            if (i++ != end) {
                b.append(", ");
            }
        }
        sci.addOption("logLevel",
                Option.builder(OPTION)
                        .longOpt(OPTION_LONG)
                        .hasArg()
                        .argName(AddonsMessages.tr("sll.argName"))
                        .desc(AddonsMessages.tr("help.sll", b.toString(), AppConfig.INSTANCE.get(CONFIG_KEY)))
                        .build(),
                this);
    }

    private void setByConfig() {
        val cfg = AppConfig.INSTANCE;
        String level = cfg.get(CONFIG_KEY);
        if (StringUtils.isNotEmpty(level)) {
            Log.setLevel(Level.forName(level, Level.DEFAULT_LEVEL));
        } else {
            cfg.set(CONFIG_KEY, Level.DEFAULT_LEVEL.getName(), String.class);
        }
    }
}
