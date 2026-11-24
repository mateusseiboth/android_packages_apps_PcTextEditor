package org.lineageos.pctexteditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.lineageos.pctexteditor.R
import org.lineageos.pctexteditor.TabDocument
import org.lineageos.pctexteditor.core.LineEnding
import org.lineageos.pctexteditor.core.TabInfo
import org.lineageos.pctexteditor.core.TextEncoding
import org.lineageos.pctexteditor.core.TextLines

data class StatusActions(
    val onReopen: (TextEncoding) -> Unit,
    val onSaveEncoding: (TextEncoding) -> Unit,
    val onLineEnding: (LineEnding) -> Unit,
)

/** Cursor position, encoding and line breaks of the selected tab. */
@Composable
fun StatusBar(tab: TabInfo, doc: TabDocument, actions: StatusActions) {
    val position = TextLines.positionOf(doc.state.text, doc.state.selection.start)
    Row(
        Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.status_position, position.line, position.column),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        EncodingMenu(tab, actions)
        LineEndingMenu(tab, actions)
    }
}

@Composable
private fun EncodingMenu(tab: TabInfo, actions: StatusActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        StatusButton(tab.encoding.label) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuHeader(R.string.encoding_reopen)
            TextEncoding.entries.forEach { encoding ->
                DropdownMenuItem(text = { Text(encoding.label) }, onClick = {
                    open = false
                    actions.onReopen(encoding)
                })
            }
            HorizontalDivider()
            MenuHeader(R.string.encoding_save)
            TextEncoding.entries.forEach { encoding ->
                DropdownMenuItem(text = { Text(encoding.label) }, onClick = {
                    open = false
                    actions.onSaveEncoding(encoding)
                })
            }
        }
    }
}

@Composable
private fun LineEndingMenu(tab: TabInfo, actions: StatusActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        StatusButton(tab.lineEnding.label) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuHeader(R.string.line_breaks)
            LineEnding.entries.forEach { ending ->
                DropdownMenuItem(text = { Text(ending.label) }, onClick = {
                    open = false
                    actions.onLineEnding(ending)
                })
            }
        }
    }
}

@Composable
private fun StatusButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 10.dp), modifier = Modifier.height(28.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MenuHeader(label: Int) {
    Text(
        stringResource(label),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}
