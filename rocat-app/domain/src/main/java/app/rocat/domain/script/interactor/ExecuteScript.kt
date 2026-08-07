package app.rocat.domain.script

import app.rocat.scripting.api.ScriptEngine
import app.rocat.scripting.api.ScriptEnvironment
import app.rocat.scripting.api.ScriptResult
import app.rocat.scripting.api.model.Script

/**
 * Use case that runs a script through the shared [ScriptEngine]. This is where the
 * engine and environment get wired together, keeping the presentation layer simple.
 */
class ExecuteScript(
    private val engine: ScriptEngine,
    private val environment: ScriptEnvironment,
) {
    suspend fun await(script: Script): ScriptResult = engine.execute(script, environment)
}