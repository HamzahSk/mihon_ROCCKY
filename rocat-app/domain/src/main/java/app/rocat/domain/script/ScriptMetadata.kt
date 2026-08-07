package app.rocat.domain.script

/**
 * Parsed `==UserScript==` metadata block, mirroring Tampermonkey/Greasemonkey headers
 * and mihon extension descriptors.
 */
data class ScriptMetadata(
    val name: String = "",
    val version: String = "0.0.0",
    val description: String = "",
    val author: String = "",
    val icon: String = "",
    /** Merged `@match` + `@include` allow-list. */
    val matches: List<String> = emptyList(),
)
