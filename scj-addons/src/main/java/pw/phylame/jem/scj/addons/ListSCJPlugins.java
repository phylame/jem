package pw.phylame.jem.scj.addons;

import org.apache.commons.cli.Option;

import lombok.val;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Command;
import pw.phylame.qaf.core.App;
import pw.phylame.qaf.core.Plugin;
import pw.phylame.ycl.util.Log;

public class ListSCJPlugins extends AbstractPlugin implements Command {
    private static final String TAG = "LSP";
    private static final String ITEM_PATTERN = "%8s: %s";

    public ListSCJPlugins() {
        super(new Metadata("List SCJ Plugins", "1.0", "PW"));
    }

    @Override
    public void init() {
        Log.i(TAG, "adding -U option to SCJ...");
        sci.addOption(Option.builder("U").longOpt("plugins").desc(AddonsMessages.tr("help.lsp")).build(), this);
    }

    @Override
    public int execute(CLIDelegate delegate) {
        val plugins = App.INSTANCE.getPlugins();
        System.out.println(AddonsMessages.tr("lsp.tip", plugins.size()));
        for (val plugin : plugins) {
            printPlugin(plugin);
        }
        return 0;
    }

    private void printPlugin(Plugin plugin) {
        String text = formatItem("id", plugin.getId());
        int width = text.length();
        System.out.println(text);
        for (val e : plugin.getMeta().entrySet()) {
            text = formatItem(e.getKey(), e.getValue());
            width = Math.max(width, text.length());
            System.out.println(text);
        }
        System.out.println(multiplyOf("-", width));
    }

    private String formatItem(String name, Object value) {
        return String.format(ITEM_PATTERN, name, value);
    }

    private String multiplyOf(String str, int count) {
        val b = new StringBuffer();
        for (int i = 0; i < count; i++) {
            b.append(str);
        }
        return b.toString();
    }
}
