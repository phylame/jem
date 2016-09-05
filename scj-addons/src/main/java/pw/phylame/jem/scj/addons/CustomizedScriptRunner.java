package pw.phylame.jem.scj.addons;

import java.io.File;
import java.io.FileReader;
import java.util.UUID;

import javax.script.ScriptEngine;

import org.apache.commons.cli.Option;

import lombok.val;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Command;
import pw.phylame.qaf.cli.TypedFetcher;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.StringUtils;

public class CustomizedScriptRunner extends AbstractPlugin {
    private static final String TAG = "CSR";

    public CustomizedScriptRunner() {
        super(new Metadata(UUID.randomUUID().toString(), "Script Runner", "1.0", "PW"));
    }

    @Override
    public void init() {
        sci.addOption(Option.builder("R")
                .hasArg()
                .argName(Messages.tr("runScript.file"))
                .longOpt("run-script")
                .desc(Messages.tr("help.runScript"))
                .build(), new RunnerCommand());
    }

    private Object detectScriptEngineManager() {
        try {
            val clazz = Class.forName("javax.script.ScriptEngineManager");
            String engine = config.rawFor("csr.engine.name");
            if (StringUtils.isEmpty(engine)) {
                engine = "JavaScript";
            }
            return clazz.getMethod("getEngineByName", String.class).invoke(clazz.newInstance(), engine);
        } catch (Exception e) {
            Log.e(TAG, e);
            app.error(Messages.tr("runScript.unsupported", System.getProperty("java.version")));
            return null;
        }
    }

    private class RunnerCommand extends TypedFetcher<String> implements Command {

        private RunnerCommand() {
            super("R", String.class, null);
        }

        @Override
        public int execute(CLIDelegate delegate) {
            val file = new File(sci.getContext().get("R").toString());
            if (!file.exists()) {
                app.error(app.tr("error.input.notExists", file.getPath()));
                return -1;
            }
            val result = detectScriptEngineManager();
            if (result == null) {
                return -1;
            }
            try (val reader = new FileReader(file)) {
                val engine = (ScriptEngine) result;
                engine.put("app", app);
                engine.put("sci", sci);
                engine.put("config", config);
                engine.eval(reader);
            } catch (Exception e) {
                app.error(Messages.tr("runScript.error"), e);
                return -1;
            }
            return 0;
        }

    }

}
