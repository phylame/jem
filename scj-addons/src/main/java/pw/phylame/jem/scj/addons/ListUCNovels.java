package pw.phylame.jem.scj.addons;

import lombok.val;
import org.apache.commons.cli.Option;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Command;
import pw.phylame.qaf.core.App;

public class ListUCNovels extends AbstractPlugin implements Command {
    private static final String TAG = "LUN";
    private static final String OPTION = "N";
    private static final String OPTION_LONG = "list-novels";

    public ListUCNovels() {
        super(new Metadata("List UC Novels", "1.0", "PW"));
    }

    @Override
    public void init() {
        sci.addOption(Option.builder(OPTION)
                .longOpt(OPTION_LONG)
                .desc(AddonsMessages.tr("help.lun"))
                .build(), this
        );
    }

    @Override
    public int execute(CLIDelegate delegate) {
        val app = App.INSTANCE;
        val inputs = sci.getInputs();
        if (inputs.length == 0) {
            app.error(app.tr("error.input.empty"));
            return -1;
        }
        app.echo("LCN is under development");
        return 0;
    }

}
