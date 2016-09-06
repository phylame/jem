package pw.phylame.jem.scj.addons;

import java.io.File;
import java.io.FileReader;

import javax.script.ScriptEngine;

import org.apache.commons.cli.Option;

import lombok.val;
import pw.phylame.qaf.cli.CLIDelegate;
import pw.phylame.qaf.cli.Command;
import pw.phylame.qaf.cli.TypedFetcher;
import pw.phylame.ycl.log.Log;
import pw.phylame.ycl.util.StringUtils;

public class ScriptRunner extends AbstractPlugin {
    private static final String TAG = "CSR";

    private static final String OPTION = "R";

    private static final String DEFAULT_ENGINE = "JavaScript";

    public ScriptRunner() {
        super(new Metadata("ff6369df-2b11-4d9d-80e2-1197fc9e088f", "Script Runner", "1.0", "PW"));
    }

    @Override
    public void init() {
        sci.addOption(Option.builder(OPTION)
                .longOpt("run-script")
                .hasArg()
                .argName(M.tr("runScript.file"))
                .desc(M.tr("help.runScript"))
                .build(), new RunnerCommand());
    }

    private Object detectScriptEngine() {
        try {
            val clazz = Class.forName("javax.script.ScriptEngineManager");
            String name = config.rawFor("csr.engine.name");
            if (StringUtils.isEmpty(name)) {
                name = DEFAULT_ENGINE;
            }
            val engine = clazz.getMethod("getEngineByName", String.class).invoke(clazz.newInstance(), name);
            if (engine == null) {
                app.error(M.tr("runScript.noSuchEngine", name));
            }
            return engine;
        } catch (Exception e) {
            Log.e(TAG, e);
            app.error(M.tr("runScript.unsupported", System.getProperty("java.version")));
            return null;
        }
    }

    private class RunnerCommand extends TypedFetcher<String> implements Command {

        private RunnerCommand() {
            super(OPTION, String.class, null);
        }

        @Override
        public int execute(CLIDelegate delegate) {
            val file = new File(sci.getContext().get(OPTION).toString());
            if (!file.exists()) {
                app.error(app.tr("error.input.notExists", file.getPath()));
                return -1;
            }
            val result = detectScriptEngine();
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
                app.error(M.tr("runScript.error"), e);
                return -1;
            }
            return 0;
        }
    }

}
