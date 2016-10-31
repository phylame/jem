package pw.phylame.jem.scj.addons;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import lombok.val;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Command;
import pw.phylame.qaf.cli.Initializer;
import pw.phylame.ycl.io.IOUtils;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.CollectUtils;

public class DisablePlugin extends AbstractPlugin implements Initializer, Command {
    private static final String TAG = DisablePlugin.class.getSimpleName();

    private static final String OPTION = "D";

    public DisablePlugin() {
        super(new Metadata(UUID.randomUUID().toString(), "Disable Plugin", "1.0", "PW"));
    }

    @Override
    public void init() {
        sci.addOption(Option.builder(OPTION)
                .longOpt("disable-plugin")
                .hasArg()
                .argName(M.tr("disablePlugin.argName"))
                .desc(M.tr("disablePlugin.tip"))
                .build(), this);
    }

    @Override
    public int execute(CLIDelegate delegate) {
        val paths = (String[]) sci.getContext().get(OPTION);
        if (paths != null && paths.length > 0) {
            try (val reader = new FileReader(config.getBlacklist())) {
                val set = CollectUtils.setOf(IOUtils.linesOf(reader, true));
                for (val path : paths) {
                    set.add(path);
                }
            } catch (FileNotFoundException e) {
                Log.d(TAG, "not found blacklist file: %s", config.getBlacklist());
            } catch (IOException e) {
                Log.d(TAG, e);
            }
        }
        return 0;
    }

    @Override
    public void perform(CLIDelegate delegate, CommandLine cmd) {
        sci.getContext().put(OPTION, cmd.getOptionValues(OPTION));
    }

}
