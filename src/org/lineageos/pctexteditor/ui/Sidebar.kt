package org.lineageos.pctexteditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.time.ZoneId
import java.util.Date
import org.lineageos.pctexteditor.R
import org.lineageos.pctexteditor.core.RecentDay
import org.lineageos.pctexteditor.core.RecentFile
import org.lineageos.pctexteditor.core.RecentFiles

data class SidebarActions(
    val onOpenFile: () -> Unit,
    val onOpenRecent: (RecentFile) -> Unit,
    val onRemoveRecent: (String) -> Unit,
)

/** "Open file" and the recent files, newest first. */
@Composable
fun Sidebar(recent: List<RecentFile>, actions: SidebarActions) {
    Column(
        Modifier.width(264.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalButton(onClick = actions.onOpenFile, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_open))
        }
        Text(
            stringResource(R.string.recent_files).uppercase(),
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RecentList(recent, actions)
    }
}

@Composable
private fun RecentList(recent: List<RecentFile>, actions: SidebarActions) {
    if (recent.isEmpty()) {
        Text(
            stringResource(R.string.recent_empty),
            modifier = Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items(recent, key = { it.uri }) { file -> RecentRow(file, actions) }
    }
}

@Composable
private fun RecentRow(file: RecentFile, actions: SidebarActions) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { actions.onOpenRecent(file) }
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                lastViewedLabel(file.openedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { actions.onRemoveRecent(file.uri) }, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.recent_remove),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun lastViewedLabel(openedAt: Long): String {
    val day = RecentFiles.dayOf(openedAt, System.currentTimeMillis(), ZoneId.systemDefault())
    val labels = mapOf(
        RecentDay.TODAY to stringResource(R.string.recent_today),
        RecentDay.YESTERDAY to stringResource(R.string.recent_yesterday),
        RecentDay.EARLIER to stringResource(R.string.recent_earlier, DateFormat.getDateInstance().format(Date(openedAt))),
    )
    return labels.getValue(day)
}
