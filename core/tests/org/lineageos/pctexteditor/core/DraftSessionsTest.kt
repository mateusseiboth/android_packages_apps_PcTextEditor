package org.lineageos.pctexteditor.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftSessionsTest {
    private val saved = DraftTab(
        id = 3, name = "notas finais.txt", uri = "content://docs/document/primary%3Anotas.txt",
        encoding = TextEncoding.WINDOWS_1252, lineEnding = LineEnding.CRLF,
        savedEncoding = TextEncoding.UTF_8, savedLineEnding = LineEnding.LF,
        stamp = FileStamp(modified = 1700000000000, size = 42, hash = null),
    )
    private val untitled = DraftTab(
        id = 7, name = "Sem título", uri = null, encoding = TextEncoding.UTF_8, lineEnding = LineEnding.LF,
        savedEncoding = TextEncoding.UTF_8, savedLineEnding = LineEnding.LF, stamp = null,
    )

    @Test
    fun formatAndParseRoundTrip() {
        val session = DraftSession(listOf(saved, untitled), selectedId = 7)
        assertEquals(session, DraftSessions.parse(DraftSessions.format(session)))
    }

    @Test
    fun brokenLinesAreSkipped() {
        val text = DraftSessions.format(DraftSession(listOf(untitled), selectedId = 7)) + "\ntab lixo\n\n"
        assertEquals(listOf(untitled), DraftSessions.parse(text).tabs)
    }

    @Test
    fun emptyOrMissingIsAnEmptySession() {
        assertEquals(DraftSession(emptyList(), null), DraftSessions.parse(""))
    }

    @Test
    fun autoSaveNeedsAFileChangesAndAPause() {
        val idle = AutoSave.IDLE_MILLIS
        assertTrue(AutoSave.isDue(enabled = true, hasFile = true, isDirty = true, lastEditAt = 0, now = idle))
        assertFalse(AutoSave.isDue(enabled = false, hasFile = true, isDirty = true, lastEditAt = 0, now = idle))
        assertFalse(AutoSave.isDue(enabled = true, hasFile = false, isDirty = true, lastEditAt = 0, now = idle))
        assertFalse(AutoSave.isDue(enabled = true, hasFile = true, isDirty = false, lastEditAt = 0, now = idle))
        assertFalse(AutoSave.isDue(enabled = true, hasFile = true, isDirty = true, lastEditAt = 0, now = idle - 1))
    }
}
