package app.rocat.scripting.api.model

import kotlinx.serialization.Serializable

/**
 * A user-supplied custom script, conceptually similar to a Tampermonkey userscript
 * or a mihon extension descriptor.
 */
@Serializable
data class Script(
    val id: String,
    val name: String,
    val version: String = "0.0.0",
    val description: String = "",
    /** The JavaScript source code. */
    val source: String,
    /** URL patterns (or host allow-list) this script is allowed to run against. */
    @Serializable(with = StringListSerializer::class)
    val matches: List<String> = emptyList(),
    /** Listed as installed/enabled by the user. */
    val enabled: Boolean = true,
)