package org.lineageos.pctexteditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.lineageos.pctexteditor.R
import org.lineageos.pctexteditor.core.TabInfo

private val TAB_SHAPE = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)

data class TabStripActions(
    val onSelect: (Long) -> Unit,
    val onClose: (Long) -> Unit,
    val onNewTab: () -> Unit,
    val onToggleSidebar: () -> Unit,
)

/** Browser-like tab strip: sidebar button, one tab per file (" *" when unsaved), new tab, and the menu. */
@Composable
fun TabStrip(
    tabs: List<TabInfo>,
    selectedId: Long?,
    isDirty: (TabInfo) -> Boolean,
    actions: TabStripActions,
    menu: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(44.dp).background(MaterialTheme.colorScheme.surfaceContainer),
        verticalAlignment = Alignment.Bottom,
    ) {
        IconButton(onClick = actions.onToggleSidebar, modifier = Modifier.align(Alignment.CenterVertically)) {
            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.action_sidebar))
        }
        Row(
            Modifier.weight(1f, fill = false).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            tabs.forEach { tab -> EditorTabChip(tab, tab.id == selectedId, isDirty(tab), actions) }
        }
        IconButton(onClick = actions.onNewTab, modifier = Modifier.align(Alignment.CenterVertically)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_new_tab))
        }
        Box(Modifier.weight(1f, fill = true))
        Box(Modifier.align(Alignment.CenterVertically)) { menu() }
    }
}

@Composable
private fun EditorTabChip(tab: TabInfo, selected: Boolean, dirty: Boolean, actions: TabStripActions) {
    val colors = MaterialTheme.colorScheme
    val background = colors.surface.takeIf { selected } ?: colors.surfaceContainer
    val title = stringResource(R.string.unsaved_title, tab.name).takeIf { dirty } ?: tab.name
    Row(
        Modifier.height(36.dp).widthIn(min = 120.dp, max = 220.dp).clip(TAB_SHAPE).background(background)
            .clickable { actions.onSelect(tab.id) }.padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = { actions.onClose(tab.id) }, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.close_tab, tab.name),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

data class MenuEntry(val label: Int, val checked: Boolean? = null, val onClick: () -> Unit)

/** The overflow menu; entries with [MenuEntry.checked] show a check mark. A null entry is a divider. */
@Composable
fun OverflowMenu(entries: List<MenuEntry?>) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_more))
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        entries.forEach { entry -> MenuRow(entry) { open = false } }
    }
}

@Composable
private fun MenuRow(entry: MenuEntry?, onDone: () -> Unit) {
    entry ?: return HorizontalDivider()
    DropdownMenuItem(
        text = { Text(stringResource(entry.label)) },
        onClick = {
            onDone()
            entry.onClick()
        },
        trailingIcon = entry.checked?.let { checked -> { CheckMark(checked) } },
    )
}

@Composable
private fun CheckMark(checked: Boolean) {
    Box(Modifier.size(20.dp)) {
        if (checked) Icon(Icons.Filled.Check, contentDescription = null)
    }
}
