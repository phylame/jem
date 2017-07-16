/*
 * Copyright 2017 Peng Wan <phylame@163.com>
 *
 * This file is part of Jem.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jem.scj.addon

import jclp.log.Log
import jem.Book
import jem.scj.app.SCI
import jem.scj.app.SCIPlugin
import jem.scj.app.SCISettings
import mala.cli.*
import mala.core.App
import mala.core.App.tr
import org.apache.commons.cli.Option
import java.io.File
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException

class ScriptRunner : ValuesFetcher("R"), SCIPlugin, Command {
    companion object {
        private const val TAG = "ScriptRunner"
    }

    override val meta = mapOf("name" to "Script Runner")

    override fun init() {
        Supports.attachTranslator()
        SCI.newOption("R", "run-script")
                .hasArg()
                .argName(tr("opt.R.arg"))
                .action(this)
        Option.builder()
                .hasArg()
                .longOpt("engine-name")
                .argName(tr("opt.engine.arg"))
                .desc(tr("opt.engineName.desc"))
                .action(RawFetcher("engine-name"))
        Option.builder()
                .hasArg()
                .argName(tr("opt.R.arg"))
                .longOpt("book-filter")
                .desc(tr("opt.bookFilter.desc"))
                .action(RawFetcher("book-filter"))
    }

    @Suppress("UNCHECKED_CAST")
    override fun execute(delegate: CLIDelegate): Int {
        val paths = SCI.context["R"] as? List<String> ?: return -1
        var code = 0
        paths.map(::File).forEach {
            if (!it.exists()) {
                App.error(tr("error.misc.noFile", it))
                code = -1
            } else {
                code = minOf(code, runScript(it))
            }
        }
        return code
    }

    override fun onBookOpened(book: Book) {
        val file = File(SCI.context["book-filter"]?.toString() ?: return)
        if (!file.exists()) {
            App.error(tr("error.misc.noFile", file))
            return
        }
        runScript(file) {
            put("book", book)
        }
    }

    private fun runScript(file: File, action: (ScriptEngine.() -> Unit)? = null): Int {
        val engine = getScriptEngine(file) ?: return -1
        Log.d(TAG, "engine {0} detected", engine)
        action?.invoke(engine)
        try {
            file.reader().use(engine::eval)
        } catch (e: ScriptException) {
            App.error(tr("error.scriptRunner.badScript"), e)
            return -1
        }
        return 0
    }

    private fun getScriptEngine(file: File): ScriptEngine? {
        val engineManager = ScriptEngineManager()
        val name = SCI.context["engine-name"]?.toString()
        val engine: ScriptEngine?
        if (name != null) {
            engine = engineManager.getEngineByName(name)
            if (engine == null) {
                App.error(tr("error.scriptRunner.noName", name))
                return null
            }
        } else {
            engine = engineManager.getEngineByExtension(file.extension)
            if (engine == null) {
                App.error(tr("error.scriptRunner.noExtension", file.extension))
                return null
            }
        }
        engine.put("app", App)
        engine.put("sci", SCI)
        engine.put("settings", SCISettings)
        return engine
    }
}
