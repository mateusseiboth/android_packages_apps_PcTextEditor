package org.lineageos.pctexteditor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.lineageos.pctexteditor.CloseAnswer
import org.lineageos.pctexteditor.EditorDialog
import org.lineageos.pctexteditor.EditorSettings
import org.lineageos.pctexteditor.EditorViewModel
import org.lineageos.pctexteditor.R
import org.lineageos.pctexteditor.ThemeMode

/** Shows the dialog the view model asks for; each kind has its own composable. */
@Composable
fun EditorDialogs(vm: EditorViewModel) {
    when (val dialog = vm.dialog) {
        is EditorDialog.ConfirmLarge -> LargeFileDialog(dialog.name, vm::confirmLarge)
        is EditorDialog.SaveChanges -> SaveChangesDialog(dialog.name, vm::answerClose)
        is EditorDialog.EncodingLoss -> EncodingLossDialog(dialog.encoding.label, vm::answerEncodingLoss)
        EditorDialog.Settings -> SettingsDialog(vm.settings, vm::updateSettings, vm::dismissDialog)
        null -> Unit
    }
}

@Composable
private fun LargeFileDialog(name: String, onAnswer: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAnswer(false) },
        title = { Text(stringResource(R.string.large_file_title)) },
        text = { Text(stringResource(R.string.large_file_message, name)) },
        confirmButton = { TextButton(onClick = { onAnswer(true) }) { Text(stringResource(R.string.open_anyway)) } },
        dismissButton = { TextButton(onClick = { onAnswer(false) }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun SaveChangesDialog(name: String, onAnswer: (CloseAnswer) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAnswer(CloseAnswer.CANCEL) },
        title = { Text(stringResource(R.string.save_changes_title)) },
        text = { Text(stringResource(R.string.save_changes_message, name)) },
        confirmButton = {
            TextButton(onClick = { onAnswer(CloseAnswer.SAVE) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onAnswer(CloseAnswer.CANCEL) }) { Text(stringResource(R.string.cancel)) }
                TextButton(onClick = { onAnswer(CloseAnswer.DISCARD) }) { Text(stringResource(R.string.dont_save)) }
            }
        },
    )
}

@Composable
private fun EncodingLossDialog(encoding: String, onAnswer: (Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAnswer(false) },
        title = { Text(stringResource(R.string.encoding_loss_title)) },
        text = { Text(stringResource(R.string.encoding_loss_message, encoding)) },
        confirmButton = { TextButton(onClick = { onAnswer(true) }) { Text(stringResource(R.string.save_utf8)) } },
        dismissButton = { TextButton(onClick = { onAnswer(false) }) { Text(stringResource(R.string.cancel)) } },
    )
}

private val THEME_LABELS = mapOf(
    ThemeMode.SYSTEM to R.string.theme_system,
    ThemeMode.LIGHT to R.string.theme_light,
    ThemeMode.DARK to R.string.theme_dark,
)

@Composable
private fun SettingsDialog(
    settings: EditorSettings,
    onChange: ((EditorSettings) -> EditorSettings) -> Unit,
    onDone: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(R.string.appearance_section)
                Text(stringResource(R.string.theme_label), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.theme == mode,
                            onClick = { onChange { it.copy(theme = mode) } },
                            label = { Text(stringResource(THEME_LABELS.getValue(mode))) },
                        )
                    }
                }
                Text(stringResource(R.string.font_size, settings.fontSize), style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = settings.fontSize.toFloat(),
                    onValueChange = { size -> onChange { it.copy(fontSize = size.roundToInt()) } },
                    valueRange = EditorSettings.FONT_SIZES.first.toFloat()..EditorSettings.FONT_SIZES.last.toFloat(),
                    steps = EditorSettings.FONT_SIZES.last - EditorSettings.FONT_SIZES.first - 1,
                )
                SectionTitle(R.string.editor_section)
                SwitchRow(R.string.action_line_numbers, settings.lineNumbers) { on -> onChange { it.copy(lineNumbers = on) } }
                SwitchRow(R.string.action_word_wrap, settings.wordWrap) { on -> onChange { it.copy(wordWrap = on) } }
                SwitchRow(R.string.show_sidebar, settings.sidebarOnStart) { on ->
                    onChange { it.copy(sidebarOnStart = on) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun SectionTitle(label: Int) {
    Text(
        stringResource(label),
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SwitchRow(label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
