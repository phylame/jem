package pw.phylame.jem.scj.addons;

import lombok.val;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Command;
import pw.phylame.qaf.cli.Initializer;
import pw.phylame.ycl.io.IOUtils;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.CollectionUtils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;

public class DisablePlugin extends AbstractPlugin implements Initializer, Command {
    private static final String TAG = DisablePlugin.class.getSimpleName();

    private static final String OPTION = "D";

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
                for (val path : paths) {
                    set.add(path);
                }
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
