package org.lineageos.pctexteditor

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.lineageos.pctexteditor.core.FileStamp
import org.lineageos.pctexteditor.core.LineEnding
import org.lineageos.pctexteditor.core.TabInfo
import org.lineageos.pctexteditor.core.TextEncoding
import org.lineageos.pctexteditor.core.Token

/** What was last read from or written to the file, to tell whether the tab has unsaved changes. */
data class SavedForm(val text: String, val encoding: TextEncoding, val lineEnding: LineEnding)

/**
 * The editable text of one tab (with its own undo history), its saved form, the file stamp seen when it was
 * last read or written (to notice changes by other apps), and its syntax highlighting.
 */
class TabDocument(text: String, saved: SavedForm, var stamp: FileStamp? = null) {
    val state = TextFieldState(text)
    var saved by mutableStateOf(saved)
    var tokens by mutableStateOf<List<Token>>(emptyList())

    fun isDirty(info: TabInfo): Boolean =
        !state.text.contentEquals(saved.text) || info.encoding != saved.encoding || info.lineEnding != saved.lineEnding

    fun markSaved(info: TabInfo) {
        saved = SavedForm(state.text.toString(), info.encoding, info.lineEnding)
    }

    fun replaceText(text: String) {
        state.setTextAndPlaceCursorAtEnd(text)
    }
}
