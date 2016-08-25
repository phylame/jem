package pw.phylame.jem.scj.addons;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import lombok.val;
import pw.phylame.jem.scj.app.AppConfig;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Initializer;
import pw.phylame.ycl.log.Level;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.StringUtils;

public class SetLogLevel extends AbstractPlugin implements Initializer {
    private static final String OPTION = "L";
    private static final String OPTION_LONG = "log-level";
    private static final String CONFIG_KEY = "app.log.level";

    public SetLogLevel() {
        super(new Metadata("367928f7-c47e-43bc-8c10-adcf51301c44", "Log Level Setter", "1.0", "PW"));
    }

    @Override
    public void init() {
        setByConfig();
        addLogOption();
    }

    @Override
    public void perform(CLIDelegate delegate, CommandLine cmd) {
        val level = Level.forName(cmd.getOptionValue(OPTION), Level.DEFAULT_LEVEL);
        sci.getContext().put(OPTION, level);
        Log.setLevel(level);
    }

    private void addLogOption() {
        sci.addOption(
                Option.builder(OPTION).longOpt(OPTION_LONG)
                        .hasArg().argName(AddonsMessages.tr("logSetter.argName")).desc(AddonsMessages
                                .tr("help.setLogLevel", makeLevelList(), AppConfig.INSTANCE.get(CONFIG_KEY)))
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
        val level = AppConfig.INSTANCE.get(CONFIG_KEY);
        if (StringUtils.isNotEmpty(level)) {
            Log.setLevel(Level.forName(level, Level.DEFAULT_LEVEL));
        }
    }
}
