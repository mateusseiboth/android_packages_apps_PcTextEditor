package org.lineageos.pctexteditor.core

/** MIME types and names for "Save as" (the system file picker needs both). */
object FileTypes {
    const val PLAIN = "text/plain"
    private const val DEFAULT_EXTENSION = "txt"

    private val MIME_BY_EXTENSION = mapOf(
        "txt" to PLAIN,
        "md" to "text/markdown",
        "csv" to "text/csv",
        "html" to "text/html",
        "htm" to "text/html",
        "css" to "text/css",
        "xml" to "text/xml",
        "json" to "application/json",
        "js" to "application/javascript",
        "kt" to "text/x-kotlin",
        "java" to "text/x-java",
        "c" to "text/x-c",
        "h" to "text/x-c",
        "py" to "text/x-python",
        "sh" to "application/x-sh",
    )

    fun mimeFor(name: String): String = MIME_BY_EXTENSION[extensionOf(name)] ?: PLAIN

    /** A name without an extension gets ".txt"; dot files such as ".bashrc" keep their name. */
    fun saveNameFor(name: String): String = name.takeIf { hasExtension(it) || it.startsWith(".") }
        ?: "$name.$DEFAULT_EXTENSION"

    private fun hasExtension(name: String) = extensionOf(name).isNotEmpty()

    private fun extensionOf(name: String): String =
        name.substringAfterLast('.', "").takeIf { name.lastIndexOf('.') > 0 }?.lowercase() ?: ""
}
