package app.rocat.domain.script

import app.rocat.scripting.api.model.Script

/**
 * Use case to install/update a script from raw JS source. The id is derived from the
 * script name to keep it stable across re-installs.
 */
class UpsertScript(private val repository: ScriptRepository) {
    suspend fun await(id: String, name: String, source: String, description: String = "") {
        val script = Script(
            id = id,
            name = name,
            source = source,
            description = description,
            matches = emptyList(),
        )
        repository.upsertScript(script)
    }
}