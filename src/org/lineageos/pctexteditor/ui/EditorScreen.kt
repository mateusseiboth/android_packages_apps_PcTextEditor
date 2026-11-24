package org.lineageos.pctexteditor.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.lineageos.pctexteditor.EditorViewModel
import org.lineageos.pctexteditor.R
import org.lineageos.pctexteditor.SaveAsRequest
import org.lineageos.pctexteditor.core.Shortcuts

/** "Save as" through the system file picker, with a name and MIME type per request. */
private class CreateTextDocument : ActivityResultContract<SaveAsRequest, Uri?>() {
    override fun createIntent(context: Context, input: SaveAsRequest): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(input.mime)
            .putExtra(Intent.EXTRA_TITLE, input.name)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
}

@Composable
fun EditorScreen(vm: EditorViewModel) {
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val openFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { vm.openUri(it) }
    }
    // The picker's answer carries no tab: remember which tab asked.
    val pendingSaveAs = remember { mutableStateOf<SaveAsRequest?>(null) }
    val saveAs = rememberLauncherForActivityResult(CreateTextDocument()) { uri ->
        pendingSaveAs.value?.let { vm.onSaveAsPicked(it.tabId, uri) }
        pendingSaveAs.value = null
    }

    LaunchedEffect(Unit) {
        for (message in vm.messages) snackbar.showSnackbar(context.getString(message.text, *message.args.toTypedArray()))
    }
    LaunchedEffect(Unit) {
        for (request in vm.saveAsRequests) {
            pendingSaveAs.value = request
            saveAs.launch(request)
        }
    }
    LaunchedEffect(Unit) {
        for (request in vm.openRequests) openFiles.launch(arrayOf("*/*"))
    }

    Scaffold(
        modifier = Modifier.onPreviewKeyEvent { event ->
            val command = event.toChord()?.let(Shortcuts::commandFor) ?: return@onPreviewKeyEvent false
            vm.run(command)
            true
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            if (vm.sidebarVisible) {
                Sidebar(
                    vm.recent,
                    SidebarActions(
                        onOpenFile = { openFiles.launch(arrayOf("*/*")) },
                        onOpenRecent = vm::openRecent,
                        onRemoveRecent = vm::removeRecent,
                    ),
                )
            }
            EditorColumn(vm, Modifier.weight(1f))
        }
    }
    EditorDialogs(vm)
}

@Composable
private fun EditorColumn(vm: EditorViewModel, modifier: Modifier) {
    val tab = vm.tabs.selected
    val doc = tab?.let { vm.document(it.id) }
    Column(modifier.fillMaxSize()) {
        TabStrip(
            tabs = vm.tabs.tabs,
            selectedId = vm.tabs.selectedId,
            isDirty = vm::isDirty,
            actions = TabStripActions(
                onSelect = vm::select,
                onClose = vm::requestClose,
                onNewTab = { vm.newTab() },
                onToggleSidebar = { vm.sidebarVisible = !vm.sidebarVisible },
            ),
            menu = { EditorMenu(vm) },
        )
        Box(Modifier.weight(1f)) {
            if (tab == null || doc == null) return@Box
            key(tab.id) {
                LaunchedEffect(doc, vm.find.visible) { snapshotFlow { doc.state.text }.collect { vm.refreshFind() } }
                EditorPane(doc, vm.settings, vm.find)
            }
            if (vm.find.visible) {
                FindBar(vm.find, findActions(vm), Modifier.align(Alignment.TopEnd).padding(12.dp))
            }
        }
        if (tab != null && doc != null) {
            StatusBar(
                tab,
                doc,
                StatusActions(
                    onReopen = { vm.reopenWithEncoding(tab.id, it) },
                    onSaveEncoding = { vm.setSaveEncoding(tab.id, it) },
                    onLineEnding = { vm.setLineEnding(tab.id, it) },
                ),
            )
        }
    }
}

@Composable
private fun EditorMenu(vm: EditorViewModel) {
    val selected = vm.tabs.selectedId
    OverflowMenu(
        listOf(
            MenuEntry(R.string.action_new_tab) { vm.newTab() },
            MenuEntry(R.string.action_open) { vm.openRequests.trySend(Unit) },
            MenuEntry(R.string.action_save) { selected?.let(vm::save) },
            MenuEntry(R.string.action_save_as) { selected?.let(vm::requestSaveAs) },
            null,
            MenuEntry(R.string.action_find) { vm.openFind(replace = false) },
            MenuEntry(R.string.action_replace) { vm.openFind(replace = true) },
            null,
            MenuEntry(R.string.action_line_numbers, checked = vm.settings.lineNumbers) {
                vm.updateSettings { it.copy(lineNumbers = !it.lineNumbers) }
            },
            MenuEntry(R.string.action_word_wrap, checked = vm.settings.wordWrap) {
                vm.updateSettings { it.copy(wordWrap = !it.wordWrap) }
            },
            null,
            MenuEntry(R.string.action_settings) { vm.showSettings() },
        ),
    )
}

private fun findActions(vm: EditorViewModel): FindActions {
    val options = { vm.find.options }
    return FindActions(
        onQuery = vm::setQuery,
        onReplacement = vm::setReplacement,
        onNext = vm::findNext,
        onPrevious = vm::findPrevious,
        onMatchCase = { vm.setFindOptions(options().copy(matchCase = !options().matchCase)) },
        onWholeWord = { vm.setFindOptions(options().copy(wholeWord = !options().wholeWord)) },
        onToggleReplace = vm::toggleReplace,
        onReplace = vm::replaceCurrent,
        onReplaceAll = vm::replaceAll,
        onClose = vm::closeFind,
    )
}
