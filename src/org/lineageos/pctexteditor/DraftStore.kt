package org.lineageos.pctexteditor

import java.io.File
import org.lineageos.pctexteditor.core.DraftSession
import org.lineageos.pctexteditor.core.DraftSessions

/** The texts kept for one tab: what is in the editor and, for a file with changes, what the file holds. */
data class DraftTexts(val current: String?, val saved: String?)

/**
 * Keeps the open tabs in the app's private files so unsaved work comes back after a restart or after the
 * system ends the process. Every call does file I/O: run it off the main thread.
 */
class DraftStore(private val dir: File) {
    private val session = File(dir, "session")

    fun load(): DraftSession {
        if (!session.isFile) return DraftSession(emptyList(), null)
        return DraftSessions.parse(session.readText())
    }

    fun textsOf(id: Long) = DraftTexts(readOrNull(textFile(id)), readOrNull(savedFile(id)))

    /** Writes the texts first and the session last (through a rename), then drops files no tab uses. */
    fun save(draft: DraftSession, texts: Map<Long, DraftTexts>) {
        dir.mkdirs()
        texts.forEach { (id, kept) ->
            writeOrDelete(textFile(id), kept.current)
            writeOrDelete(savedFile(id), kept.saved)
        }
        val pending = File(dir, "session.new")
        pending.writeText(DraftSessions.format(draft))
        pending.renameTo(session)
        val used = texts.keys.flatMap { listOf(textFile(it).name, savedFile(it).name) }.toSet() + session.name
        dir.listFiles()?.filterNot { it.name in used }?.forEach { it.delete() }
    }

    private fun textFile(id: Long) = File(dir, "$id.text")

    private fun savedFile(id: Long) = File(dir, "$id.saved")

    private fun readOrNull(file: File): String? = file.takeIf { it.isFile }?.readText()

    private fun writeOrDelete(file: File, text: String?) {
        text?.let { file.writeText(it) } ?: file.delete()
    }
}
