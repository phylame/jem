package pw.phylame.jem.scj.addons;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import lombok.NonNull;
import lombok.val;
import pw.phylame.jem.scj.app.AppConfig;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Initializer;
import pw.phylame.ycl.util.Log;
import pw.phylame.ycl.util.StringUtils;

public class SetLogLevel extends AbstractPlugin implements Initializer {
    private static final String TAG = "SLL";

    public SetLogLevel() {
        super(new Metadata("Set Log Level", "1.0", "PW"));
    }

    @Override
    public void init() {
        setByConfig();
        addLogOption();
    }

    @Override
    public void perform(CLIDelegate arg0, CommandLine arg1) {
        // TODO Auto-generated method stub

    }

    private void addLogOption() {
        Log.i(TAG, "adding -L option to SCJ...");
        sci.addOption("logLevel", Option.builder("L").longOpt("level").desc(AddonsMessages.tr("help.sll")).build(),
                this);
    }

    private void setByConfig() {
        val cfg = AppConfig.INSTANCE;
        String level = cfg.get("app.log.level");
        if (StringUtils.isNotEmpty(level)) {
            Log.setLevel(parseLevel(level));
        }
    }

    private int parseLevel(@NonNull String levle) {
        switch (levle.toLowerCase()) {
        case "all":
            return Log.ALL;
        case "trace":
            return Log.TRACE;
        case "debug":
            return Log.DEBUG;
        case "info":
            return Log.INFO;
        case "warn":
            return Log.WARN;
        case "error":
            return Log.ERROR;
        case "fatal":
            return Log.FATAL;
        case "off":
            return Log.OFF;
        default:
            return -1;
        }
    }

}
