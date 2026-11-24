package org.lineageos.pctexteditor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.lineageos.pctexteditor.FindUi
import org.lineageos.pctexteditor.R

data class FindActions(
    val onQuery: (String) -> Unit,
    val onReplacement: (String) -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onMatchCase: () -> Unit,
    val onWholeWord: () -> Unit,
    val onToggleReplace: () -> Unit,
    val onReplace: () -> Unit,
    val onReplaceAll: () -> Unit,
    val onClose: () -> Unit,
)

/** A floating card at the top right of the editor: find row, and the replace row when expanded. */
@Composable
fun FindBar(find: FindUi, actions: FindActions, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Surface(
        modifier = modifier.widthIn(max = 520.dp).onPreviewKeyEvent { event ->
            (event.key == Key.Escape).also { closes -> if (closes) actions.onClose() }
        },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FindRow(find, actions, Modifier.focusRequester(focus))
            if (find.showReplace) ReplaceRow(find, actions)
        }
    }
}

@Composable
private fun FindRow(find: FindUi, actions: FindActions, fieldModifier: Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val toggleIcon = Icons.Filled.UnfoldLess.takeIf { find.showReplace } ?: Icons.Filled.UnfoldMore
        val toggleLabel = R.string.replace_hide.takeIf { find.showReplace } ?: R.string.replace_show
        IconButton(onClick = actions.onToggleReplace) {
            Icon(toggleIcon, contentDescription = stringResource(toggleLabel))
        }
        OutlinedTextField(
            value = find.query,
            onValueChange = actions.onQuery,
            modifier = fieldModifier.width(240.dp),
            placeholder = { Text(stringResource(R.string.find_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { actions.onNext() }),
            suffix = { MatchCount(find) },
        )
        OptionToggle("Aa", R.string.match_case, find.options.matchCase, actions.onMatchCase)
        OptionToggle("ab", R.string.whole_word, find.options.wholeWord, actions.onWholeWord)
        IconButton(onClick = actions.onPrevious) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.find_previous))
        }
        IconButton(onClick = actions.onNext) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.find_next))
        }
        IconButton(onClick = actions.onClose) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.find_close))
        }
    }
}

@Composable
private fun MatchCount(find: FindUi) {
    if (find.query.isEmpty()) return
    Text(
        stringResource(R.string.match_count, find.result.position, find.result.count),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun OptionToggle(symbol: String, label: Int, checked: Boolean, onToggle: () -> Unit) {
    val description = stringResource(label)
    IconToggleButton(
        checked = checked,
        onCheckedChange = { onToggle() },
        modifier = Modifier.size(40.dp).semantics { contentDescription = description },
    ) {
        Text(symbol, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ReplaceRow(find: FindUi, actions: FindActions) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = find.replacement,
            onValueChange = actions.onReplacement,
            modifier = Modifier.padding(start = 48.dp).width(240.dp),
            placeholder = { Text(stringResource(R.string.replace_hint)) },
            singleLine = true,
        )
        TextButton(onClick = actions.onReplace, enabled = find.result.currentMatch != null) {
            Text(stringResource(R.string.action_replace))
        }
        TextButton(onClick = actions.onReplaceAll, enabled = find.result.count > 0) {
            Text(stringResource(R.string.action_replace_all))
        }
    }
}
