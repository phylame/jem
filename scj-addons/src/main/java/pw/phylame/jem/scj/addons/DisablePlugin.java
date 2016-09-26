package pw.phylame.jem.scj.addons;

import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import lombok.val;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Command;
import pw.phylame.qaf.cli.Initializer;

public class DisablePlugin extends AbstractPlugin implements Initializer, Command {
    private static final String OPTION = "D";

    public DisablePlugin() {
        super(new Metadata(UUID.randomUUID().toString(), "Disable Plugin", "1.0", "PW"));
    }

    @Override
    public void init() {
        sci.addOption(Option.builder(OPTION)
                .longOpt("disable-plugin")
                .hasArg()
                .argName(M._("disablePlugin.argName"))
                .desc(M._("disablePlugin.tip"))
                .build(), this);
    }

    @Override
    public int execute(CLIDelegate delegate) {
        val paths = (String[]) sci.getContext().get(OPTION);
        if (paths != null && paths.length > 0) {
            for (String path : paths) {

            }
        }
        return 0;
    }

    @Override
    public void perform(CLIDelegate delegate, CommandLine cmd) {
        sci.getContext().put(OPTION, cmd.getOptionValues(OPTION));
    }

}
