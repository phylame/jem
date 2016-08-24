package pw.phylame.jem.scj.addons;

import org.apache.commons.cli.Option;

import lombok.val;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Command;
import pw.phylame.qaf.core.App;
import pw.phylame.ycl.util.Log;

public class ListUCNovels extends AbstractPlugin implements Command {
    private static final String TAG = "LUN";

    public ListUCNovels() {
        super(new Metadata("List UC Novels", "1.0", "PW"));
    }

    @Override
    public void init() {
        Log.i(TAG, "adding... -N option to SCJ");
        sci.addOption(Option.builder("N").longOpt("novels").desc(AddonsMessages.tr("help.lun")).build(), this);
    }

    @Override
    public int execute(CLIDelegate delegate) {
        val app = App.INSTANCE;
        val inputs = sci.getInputs();
        if (inputs.length == 0) {
            app.error(app.tr("error.input.empty"));
            return -1;
        }
        app.echo("under development");
        return 0;
    }

}
