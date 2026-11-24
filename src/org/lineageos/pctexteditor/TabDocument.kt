package org.lineageos.pctexteditor

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.lineageos.pctexteditor.core.LineEnding
import org.lineageos.pctexteditor.core.TabInfo
import org.lineageos.pctexteditor.core.TextEncoding

/** What was last read from or written to the file, to tell whether the tab has unsaved changes. */
data class SavedForm(val text: String, val encoding: TextEncoding, val lineEnding: LineEnding)

/** The editable text of one tab (with its own undo history) and its saved form. */
class TabDocument(text: String, saved: SavedForm) {
    val state = TextFieldState(text)
    var saved by mutableStateOf(saved)

    fun isDirty(info: TabInfo): Boolean =
        !state.text.contentEquals(saved.text) || info.encoding != saved.encoding || info.lineEnding != saved.lineEnding

    fun markSaved(info: TabInfo) {
        saved = SavedForm(state.text.toString(), info.encoding, info.lineEnding)
    }

    fun replaceText(text: String) {
        state.setTextAndPlaceCursorAtEnd(text)
    }
}
