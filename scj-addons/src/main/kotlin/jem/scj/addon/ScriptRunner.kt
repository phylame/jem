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
import jem.epm.EpmManager
import jem.epm.ParserParam
import jem.scj.SCI
import jem.scj.SCIPlugin
import jem.scj.SCISettings
import mala.App
import mala.App.tr
import mala.cli.*
import org.apache.commons.cli.Option
import java.io.File
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager
import javax.script.ScriptException

class ScriptRunner : ListFetcher("R"), SCIPlugin, Command {
//    override val meta = mapOf("name" to "Script Runner")

    override fun init() {
        attachTranslator()
        SCI.newOption("R", "run-script")
                .hasArg()
                .argName(tr("opt.R.arg"))
                .action(this)
        Option.builder()
                .hasArg()
                .longOpt("engine-name")
                .argName(tr("opt.engine.arg"))
                .desc(tr("opt.engineName.desc"))
                .action(StringFetcher("engine-name"))
        Option.builder()
                .hasArg()
                .argName(tr("opt.R.arg"))
                .longOpt("book-filter")
                .desc(tr("opt.bookFilter.desc"))
                .action(StringFetcher("book-filter"))
    }

    @Suppress("UNCHECKED_CAST")
    override fun execute(delegate: CDelegate): Int {
        val paths = SCI["R"] as? List<String> ?: return -1
        var code = 0
        paths.map(::File).forEach {
            code = if (!it.exists()) {
                App.error(tr("err.misc.noFile", it))
                -1
            } else {
                minOf(code, runScript(it))
            }
        }
        return code
    }

    override fun onBookOpened(book: Book, param: ParserParam?) {
        val file = File(SCI["book-filter"]?.toString() ?: return)
        if (!file.exists()) {
            App.error(tr("err.misc.noFile", file))
            return
        }
        runScript(file) {
            put("book", book)
        }
    }

    private fun runScript(file: File, action: (ScriptEngine.() -> Unit)? = null): Int {
        val engine = getScriptEngine(file) ?: return -1
        Log.d(javaClass.simpleName) { "engine $engine detected" }
        action?.invoke(engine)
        try {
            file.reader().use(engine::eval)
        } catch (e: ScriptException) {
            App.error(tr("err.scriptRunner.badScript"), e)
            return -1
        }
        return 0
    }

    private fun getScriptEngine(file: File): ScriptEngine? {
        val engineManager = ScriptEngineManager()
        val name = SCI["engine-name"]?.toString()
        val engine: ScriptEngine?
        if (name != null) {
            engine = engineManager.getEngineByName(name)
            if (engine == null) {
                App.error(tr("err.scriptRunner.noName", name))
                return null
            }
        } else {
            engine = engineManager.getEngineByExtension(file.extension)
            if (engine == null) {
                App.error(tr("err.scriptRunner.noExtension", file.extension))
                return null
            }
        }
        engine.put("app", App)
        engine.put("sci", SCI)
        engine.put("epm", EpmManager)
        engine.put("settings", SCISettings)
        return engine
    }
}
