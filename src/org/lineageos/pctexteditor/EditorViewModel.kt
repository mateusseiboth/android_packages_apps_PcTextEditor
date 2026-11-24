package org.lineageos.pctexteditor

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.snapshotFlow
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.pctexteditor.core.AutoSave
import org.lineageos.pctexteditor.core.ChangeCheck
import org.lineageos.pctexteditor.core.DraftSession
import org.lineageos.pctexteditor.core.DraftTab
import org.lineageos.pctexteditor.core.ExternalChange
import org.lineageos.pctexteditor.core.FileStamp
import org.lineageos.pctexteditor.core.DecodedText
import org.lineageos.pctexteditor.core.EditorCommand
import org.lineageos.pctexteditor.core.EditorTabs
import org.lineageos.pctexteditor.core.FileTypes
import org.lineageos.pctexteditor.core.FindOptions
import org.lineageos.pctexteditor.core.FindState
import org.lineageos.pctexteditor.core.LineEnding
import org.lineageos.pctexteditor.core.OpenDecision
import org.lineageos.pctexteditor.core.OpenPolicy
import org.lineageos.pctexteditor.core.RecentFile
import org.lineageos.pctexteditor.core.RecentFiles
import org.lineageos.pctexteditor.core.TabInfo
import org.lineageos.pctexteditor.core.TextCodec
import org.lineageos.pctexteditor.core.TextEncoding
import org.lineageos.pctexteditor.core.TextReplace
import org.lineageos.pctexteditor.core.UntitledNames

data class UiMessage(@param:StringRes val text: Int, val args: List<Any> = emptyList())

data class SaveAsRequest(val tabId: Long, val name: String, val mime: String)

data class FindUi(
    val visible: Boolean = false,
    val showReplace: Boolean = false,
    val query: String = "",
    val replacement: String = "",
    val options: FindOptions = FindOptions(),
    val result: FindState = FindState("", emptyList(), 0),
)

sealed interface EditorDialog {
    data class ConfirmLarge(val uri: Uri, val name: String) : EditorDialog
    data class SaveChanges(val tabId: Long, val name: String) : EditorDialog
    data class EncodingLoss(val tabId: Long, val encoding: TextEncoding) : EditorDialog
    data object Settings : EditorDialog
    data class FileChanged(val tabId: Long, val name: String) : EditorDialog
}

enum class CloseAnswer { SAVE, DISCARD, CANCEL }

/** What to keep when another app changed a file the tab has unsaved changes for. */
enum class ChangeAnswer { LOAD_THEIRS, KEEP_MINE, COPY_MINE }

private sealed interface OpenOutcome {
    data class Loaded(val meta: DocumentMeta, val decoded: DecodedText, val stamp: FileStamp?) : OpenOutcome
    data class Refused(val meta: DocumentMeta, val decision: OpenDecision) : OpenOutcome
}

/** The editor's state: tabs and their documents, find/replace, dialogs, settings and recent files. */
class EditorViewModel(app: Application) : AndroidViewModel(app) {
    private val store = DocumentStore(app.contentResolver)
    private val settingsStore = SettingsStore(app.getSharedPreferences("editor", Context.MODE_PRIVATE))
    private val documents = mutableStateMapOf<Long, TabDocument>()
    private val closeAfterSave = mutableSetOf<Long>()
    private val saving = mutableSetOf<Long>()
    private val drafts = DraftStore(File(app.filesDir, "drafts"))
    private var nextId = 1L
    private var lastEditAt = 0L

    var tabs by mutableStateOf(EditorTabs.EMPTY)
        private set
    var recent by mutableStateOf(settingsStore.readRecent())
        private set
    var settings by mutableStateOf(settingsStore.readSettings())
        private set
    var sidebarVisible by mutableStateOf(settings.sidebarOnStart)
    var find by mutableStateOf(FindUi())
        private set
    var dialog by mutableStateOf<EditorDialog?>(null)
        private set

    val messages = Channel<UiMessage>(Channel.BUFFERED)
    val saveAsRequests = Channel<SaveAsRequest>(Channel.BUFFERED)
    val openRequests = Channel<Unit>(Channel.CONFLATED)

    private val refusals: Map<OpenDecision, (Uri, DocumentMeta) -> Unit> = mapOf(
        OpenDecision.NOT_TEXT to { _, meta -> message(R.string.error_not_text, meta.name) },
        OpenDecision.TOO_LARGE to { _, meta -> message(R.string.error_too_large, meta.name) },
        OpenDecision.CONFIRM_LARGE to { uri, meta -> dialog = EditorDialog.ConfirmLarge(uri, meta.name) },
    )

    private val commands: Map<EditorCommand, () -> Unit> = mapOf(
        EditorCommand.NEW_TAB to { newTab() },
        EditorCommand.OPEN to { openRequests.trySend(Unit) },
        EditorCommand.SAVE to { withSelected(::save) },
        EditorCommand.SAVE_AS to { withSelected(::requestSaveAs) },
        EditorCommand.CLOSE_TAB to { withSelected(::requestClose) },
        EditorCommand.FIND to { openFind(replace = false) },
        EditorCommand.REPLACE to { openFind(replace = true) },
        EditorCommand.FIND_NEXT to { findNext() },
        EditorCommand.FIND_PREVIOUS to { findPrevious() },
        EditorCommand.NEXT_TAB to { tabs = tabs.selectNext() },
        EditorCommand.PREVIOUS_TAB to { tabs = tabs.selectPrevious() },
    )

    private val changeActions: Map<ExternalChange, (TabInfo) -> Unit> = mapOf(
        ExternalChange.NONE to {},
        ExternalChange.RELOAD to { tab -> reload(tab, announce = true) },
        ExternalChange.ASK to { tab -> dialog = EditorDialog.FileChanged(tab.id, tab.name) },
        ExternalChange.GONE to { tab -> message(R.string.file_gone, tab.name) },
    )

    init {
        viewModelScope.launch {
            restoreDrafts()
            keepDraftsAndAutoSave()
        }
    }

    fun run(command: EditorCommand) = commands.getValue(command)()

    fun document(id: Long): TabDocument? = documents[id]

    fun isDirty(tab: TabInfo): Boolean = documents[tab.id]?.isDirty(tab) ?: false

    fun select(id: Long) {
        tabs = tabs.select(id)
        refreshFind()
    }

    // Tabs and files

    fun newTab(text: String = "") {
        val name = UntitledNames.next(tabs.tabs.map { it.name }, string(R.string.untitled))
        val info = TabInfo(nextId++, name, uri = null)
        addTab(info, text, SavedForm("", info.encoding, info.lineEnding))
    }

    fun openUri(uri: Uri, confirmed: Boolean = false) {
        tabs.tabs.firstOrNull { it.uri == uri.toString() }?.let { return select(it.id) }
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) { runCatching { readForOpen(uri, confirmed) } }
            outcome.onSuccess { applyOpen(uri, it) }.onFailure { onOpenFailed(uri) }
        }
    }

    fun openRecent(file: RecentFile) = openUri(Uri.parse(file.uri))

    fun removeRecent(uri: String) = updateRecent(RecentFiles.remove(recent, uri))

    fun confirmLarge(answer: Boolean) {
        val pending = dialog as? EditorDialog.ConfirmLarge ?: return
        dialog = null
        pending.takeIf { answer }?.let { openUri(it.uri, confirmed = true) }
    }

    fun requestClose(id: Long) {
        val tab = tabs.tabs.firstOrNull { it.id == id } ?: return
        if (isDirty(tab)) {
            dialog = EditorDialog.SaveChanges(id, tab.name)
            return
        }
        removeTab(id)
    }

    fun answerClose(answer: CloseAnswer) {
        val pending = dialog as? EditorDialog.SaveChanges ?: return
        dialog = null
        val actions = mapOf(
            CloseAnswer.SAVE to { closeAfterSave += pending.tabId; save(pending.tabId) },
            CloseAnswer.DISCARD to { removeTab(pending.tabId) },
            CloseAnswer.CANCEL to {},
        )
        actions.getValue(answer)()
    }

    fun save(id: Long) {
        val tab = tabs.tabs.firstOrNull { it.id == id } ?: return
        val uri = tab.uri ?: return requestSaveAs(id)
        val text = documents[id]?.state?.text?.toString() ?: return
        if (!TextCodec.canEncode(text, tab.encoding)) {
            dialog = EditorDialog.EncodingLoss(id, tab.encoding)
            return
        }
        write(tab, Uri.parse(uri), text)
    }

    fun requestSaveAs(id: Long) {
        val tab = tabs.tabs.firstOrNull { it.id == id } ?: return
        val name = FileTypes.saveNameFor(tab.name)
        saveAsRequests.trySend(SaveAsRequest(id, name, FileTypes.mimeFor(name)))
    }

    fun onSaveAsPicked(id: Long, uri: Uri?) {
        if (uri == null) {
            closeAfterSave -= id
            return
        }
        viewModelScope.launch {
            val meta = withContext(Dispatchers.IO) {
                store.keepAccess(uri)
                runCatching { store.metaOf(uri) }.getOrNull()
            }
            val name = meta?.name ?: tabs.tabs.firstOrNull { it.id == id }?.name ?: return@launch
            tabs = tabs.update(id) { it.copy(name = name, uri = uri.toString()) }
            rememberRecent(uri, name)
            save(id)
        }
    }

    fun answerEncodingLoss(saveInUtf8: Boolean) {
        val pending = dialog as? EditorDialog.EncodingLoss ?: return
        dialog = null
        if (!saveInUtf8) {
            closeAfterSave -= pending.tabId
            return
        }
        tabs = tabs.update(pending.tabId) { it.copy(encoding = TextEncoding.UTF_8) }
        save(pending.tabId)
    }

    fun setSaveEncoding(id: Long, encoding: TextEncoding) {
        tabs = tabs.update(id) { it.copy(encoding = encoding) }
    }

    fun setLineEnding(id: Long, ending: LineEnding) {
        tabs = tabs.update(id) { it.copy(lineEnding = ending) }
    }

    /** Reads the file again with [encoding]; the tab's unsaved changes are replaced. */
    fun reopenWithEncoding(id: Long, encoding: TextEncoding) {
        val tab = tabs.tabs.firstOrNull { it.id == id } ?: return
        val uri = tab.uri?.let(Uri::parse) ?: return setSaveEncoding(id, encoding)
        viewModelScope.launch {
            val read = withContext(Dispatchers.IO) { runCatching { TextCodec.decode(store.readAll(uri), encoding) } }
            read.onSuccess { decoded -> applyReopen(id, decoded) }.onFailure { message(R.string.error_open, tab.name) }
        }
    }

    // Changes made by other apps

    /** Compares each open file with the stamp last seen; called when the window comes back and every few seconds. */
    fun checkExternalChanges() {
        val watched = tabs.tabs.filter { it.uri != null && it.id !in saving && documents[it.id]?.stamp != null }
        if (watched.isEmpty()) return
        viewModelScope.launch {
            val stamps = withContext(Dispatchers.IO) { watched.associate { it.id to store.stampOf(Uri.parse(it.uri)) } }
            watched.forEach { onStamp(it.id, stamps[it.id]) }
        }
    }

    fun answerFileChanged(answer: ChangeAnswer) {
        val pending = dialog as? EditorDialog.FileChanged ?: return
        dialog = null
        val tab = tabs.tabs.firstOrNull { it.id == pending.tabId } ?: return
        val actions = mapOf(
            ChangeAnswer.LOAD_THEIRS to { reload(tab, announce = false) },
            ChangeAnswer.KEEP_MINE to { save(tab.id) },
            ChangeAnswer.COPY_MINE to { requestSaveAs(tab.id) },
        )
        actions.getValue(answer)()
    }

    /** Writes the drafts right away (the window is going to the background). */
    fun saveDraftsNow() {
        val (session, texts) = buildDrafts()
        DRAFT_WRITER.launch { runCatching { drafts.save(session, texts) } }
    }

    // Find and replace

    fun openFind(replace: Boolean) {
        val selected = selectedDocument()?.let { selectedText(it.state) }.orEmpty()
        val query = selected.takeIf { it.isNotEmpty() && '\n' !in it } ?: find.query
        find = find.copy(visible = true, showReplace = replace, query = query)
        refreshFind()
    }

    fun closeFind() {
        find = find.copy(visible = false)
    }

    fun toggleReplace() {
        find = find.copy(showReplace = !find.showReplace)
    }

    fun setQuery(query: String) {
        find = find.copy(query = query)
        refreshFind()
    }

    fun setReplacement(replacement: String) {
        find = find.copy(replacement = replacement)
    }

    fun setFindOptions(options: FindOptions) {
        find = find.copy(options = options)
        refreshFind()
    }

    /** Matches are counted again from the cursor whenever the text or the query changes. */
    fun refreshFind() {
        if (!find.visible) return
        val doc = selectedDocument() ?: return
        val text = doc.state.text.toString()
        find = find.copy(result = FindState.of(text, find.query, find.options, doc.state.selection.min))
    }

    fun findNext() = moveMatch(find.result.next())

    fun findPrevious() = moveMatch(find.result.previous())

    fun replaceCurrent() {
        val doc = selectedDocument() ?: return
        val match = find.result.currentMatch ?: return
        val result = TextReplace.replaceCurrent(doc.state.text.toString(), find.result, find.replacement)
        doc.state.edit {
            replace(match.first, match.last + 1, find.replacement)
            selection = TextRange(result.cursor)
        }
        refreshFind()
    }

    fun replaceAll() {
        val doc = selectedDocument() ?: return
        val result = TextReplace.replaceAll(doc.state.text.toString(), find.query, find.replacement, find.options)
        if (result.count == 0) return
        doc.state.edit {
            replace(0, length, result.text)
            selection = TextRange(0)
        }
        refreshFind()
        message(R.string.replaced_count, result.count)
    }

    // Settings

    fun showSettings() {
        dialog = EditorDialog.Settings
    }

    fun dismissDialog() {
        dialog = null
    }

    fun updateSettings(transform: (EditorSettings) -> EditorSettings) {
        settings = transform(settings)
        settingsStore.saveSettings(settings)
    }

    // Internals

    private fun withSelected(action: (Long) -> Unit) {
        tabs.selectedId?.let(action)
    }

    private fun selectedDocument(): TabDocument? = tabs.selectedId?.let { documents[it] }

    private fun selectedText(state: TextFieldState): String =
        state.text.substring(state.selection.min, state.selection.max)

    private fun moveMatch(next: FindState) {
        find = find.copy(result = next)
        val match = next.currentMatch ?: return
        selectedDocument()?.state?.edit { selection = TextRange(match.first, match.last + 1) }
    }

    private fun addTab(info: TabInfo, text: String, saved: SavedForm, stamp: FileStamp? = null) {
        documents[info.id] = TabDocument(text, saved, stamp)
        tabs = tabs.open(info)
        refreshFind()
    }

    private fun removeTab(id: Long) {
        tabs = tabs.close(id)
        documents.remove(id)
        if (tabs.tabs.isEmpty()) newTab()
        refreshFind()
    }

    /** The empty tab the editor starts with gives way to the first file opened. */
    private fun dropBlankStartTab() {
        val only = tabs.tabs.singleOrNull() ?: return
        val isBlank = only.uri == null && documents[only.id]?.state?.text?.isEmpty() == true && !isDirty(only)
        if (!isBlank) return
        tabs = tabs.close(only.id)
        documents.remove(only.id)
    }

    private fun readForOpen(uri: Uri, confirmed: Boolean): OpenOutcome {
        val meta = store.metaOf(uri)
        val decision = OpenPolicy.decide(meta.size, store.readHead(uri, OpenPolicy.HEAD_BYTES))
        val effective = OpenDecision.OPEN.takeIf { confirmed && decision == OpenDecision.CONFIRM_LARGE } ?: decision
        if (effective != OpenDecision.OPEN) return OpenOutcome.Refused(meta, effective)
        store.keepAccess(uri)
        return OpenOutcome.Loaded(meta, TextCodec.decode(store.readAll(uri)), store.stampOf(uri))
    }

    private fun applyOpen(uri: Uri, outcome: OpenOutcome) {
        if (outcome is OpenOutcome.Refused) return refusals.getValue(outcome.decision)(uri, outcome.meta)
        val loaded = outcome as OpenOutcome.Loaded
        val decoded = loaded.decoded
        dropBlankStartTab()
        val info = TabInfo(nextId++, loaded.meta.name, uri.toString(), decoded.encoding, decoded.lineEnding)
        addTab(info, decoded.text, SavedForm(decoded.text, decoded.encoding, decoded.lineEnding), loaded.stamp)
        rememberRecent(uri, loaded.meta.name)
    }

    private fun onOpenFailed(uri: Uri) {
        val name = recent.firstOrNull { it.uri == uri.toString() }?.name ?: uri.lastPathSegment.orEmpty()
        removeRecent(uri.toString())
        message(R.string.error_open, name)
    }

    private fun applyReopen(id: Long, decoded: DecodedText) {
        val doc = documents[id] ?: return
        doc.replaceText(decoded.text)
        tabs = tabs.update(id) { it.copy(encoding = decoded.encoding, lineEnding = decoded.lineEnding) }
        doc.saved = SavedForm(decoded.text, decoded.encoding, decoded.lineEnding)
        refreshFind()
    }

    /** While a tab is being written, change checks skip it: the new date is ours, not another app's. */
    private fun write(tab: TabInfo, uri: Uri, text: String, quiet: Boolean = false) {
        val bytes = TextCodec.encode(text, tab.encoding, tab.lineEnding)
        saving += tab.id
        viewModelScope.launch {
            val written = withContext(Dispatchers.IO) { runCatching { store.write(uri, bytes).let { store.stampOf(uri) } } }
            saving -= tab.id
            written.onSuccess { stamp -> onSaved(tab, text, stamp, quiet) }.onFailure {
                closeAfterSave -= tab.id
                message(R.string.error_save, tab.name)
            }
        }
    }

    /** The saved form is the text that was written, not what was typed meanwhile. */
    private fun onSaved(written: TabInfo, text: String, stamp: FileStamp?, quiet: Boolean) {
        documents[written.id]?.let { doc ->
            doc.saved = SavedForm(text, written.encoding, written.lineEnding)
            doc.stamp = stamp
        }
        if (!quiet) message(R.string.saved, written.name)
        if (closeAfterSave.remove(written.id)) removeTab(written.id)
    }

    /** The answer is reported once: the stamp seen becomes the known one. A pending dialog waits its turn. */
    private fun onStamp(id: Long, current: FileStamp?) {
        if (dialog != null) return
        val tab = tabs.tabs.firstOrNull { it.id == id } ?: return
        val doc = documents[id] ?: return
        val change = ChangeCheck.decide(doc.stamp, current, doc.isDirty(tab))
        doc.stamp = current
        changeActions.getValue(change)(tab)
    }

    /** Loads the file's new content as an edit, so undo brings the previous text back. */
    private fun reload(tab: TabInfo, announce: Boolean) {
        val uri = tab.uri?.let(Uri::parse) ?: return
        viewModelScope.launch {
            val read = withContext(Dispatchers.IO) {
                runCatching { TextCodec.decode(store.readAll(uri)) to store.stampOf(uri) }
            }
            read.onSuccess { (decoded, stamp) -> applyReload(tab, decoded, stamp, announce) }
                .onFailure { message(R.string.error_open, tab.name) }
        }
    }

    private fun applyReload(tab: TabInfo, decoded: DecodedText, stamp: FileStamp?, announce: Boolean) {
        val doc = documents[tab.id] ?: return
        val cursor = doc.state.selection.min.coerceAtMost(decoded.text.length)
        doc.state.edit {
            replace(0, length, decoded.text)
            selection = TextRange(cursor)
        }
        tabs = tabs.update(tab.id) { it.copy(encoding = decoded.encoding, lineEnding = decoded.lineEnding) }
        doc.saved = SavedForm(decoded.text, decoded.encoding, decoded.lineEnding)
        doc.stamp = stamp
        refreshFind()
        if (announce) message(R.string.file_reloaded, tab.name)
    }

    // Drafts and auto-save

    /** Brings back the tabs of the last session; clean file tabs are read again from their files. */
    private suspend fun restoreDrafts() {
        val (session, restored) = withContext(Dispatchers.IO) {
            val session = runCatching { drafts.load() }.getOrDefault(DraftSession(emptyList(), null))
            session to session.tabs.map { it to drafts.textsOf(it.id) }
        }
        val newIds = restored.mapNotNull { (draft, texts) -> restoreTab(draft, texts)?.let { draft.id to it } }.toMap()
        if (tabs.tabs.isEmpty()) newTab()
        session.selectedId?.let(newIds::get)?.let { tabs = tabs.select(it) }
    }

    private fun restoreTab(draft: DraftTab, texts: DraftTexts): Long? {
        val uri = draft.uri
        if (uri != null && tabs.tabs.any { it.uri == uri }) return null
        val current = texts.current ?: return reopenClean(uri)
        val info = TabInfo(nextId++, draft.name, uri, draft.encoding, draft.lineEnding)
        val savedText = texts.saved ?: current.takeIf { uri != null } ?: ""
        addTab(info, current, SavedForm(savedText, draft.savedEncoding, draft.savedLineEnding), draft.stamp)
        return info.id
    }

    /** A clean file tab is read again from its file; a blank untitled tab is not brought back. */
    private fun reopenClean(uri: String?): Long? {
        uri?.let { openUri(Uri.parse(it)) }
        return null
    }

    /**
     * Every change (typing, tabs) is kept as a draft after a short pause, and tabs with a file are written
     * back once typing has paused long enough, when auto-save is on.
     */
    @OptIn(FlowPreview::class)
    private fun keepDraftsAndAutoSave() {
        val changes = snapshotFlow { tabs to documents.values.map { System.identityHashCode(it.state.text) } }
            .drop(1)
            .onEach { lastEditAt = System.currentTimeMillis() }
        viewModelScope.launch { changes.debounce(DRAFT_DELAY_MILLIS).collect { saveDrafts() } }
        viewModelScope.launch { changes.debounce(AutoSave.IDLE_MILLIS).collect { autoSave() } }
    }

    private suspend fun saveDrafts() {
        val (session, texts) = buildDrafts()
        withContext(Dispatchers.IO) { runCatching { drafts.save(session, texts) } }
    }

    /** Text is kept for untitled tabs and tabs with changes; clean file tabs only need their file. */
    private fun buildDrafts(): Pair<DraftSession, Map<Long, DraftTexts>> {
        val kept = tabs.tabs.mapNotNull { tab -> documents[tab.id]?.let { tab to it } }
        val session = DraftSession(kept.map { (tab, doc) -> draftOf(tab, doc) }, tabs.selectedId)
        val texts = kept.associate { (tab, doc) ->
            val keepText = tab.uri == null || doc.isDirty(tab)
            val current = doc.state.text.toString().takeIf { keepText }
            tab.id to DraftTexts(current, doc.saved.text.takeIf { keepText && tab.uri != null })
        }
        return session to texts
    }

    private fun draftOf(tab: TabInfo, doc: TabDocument) = DraftTab(
        tab.id, tab.name, tab.uri, tab.encoding, tab.lineEnding, doc.saved.encoding, doc.saved.lineEnding, doc.stamp,
    )

    /** Auto-save never asks: text the encoding can't hold waits for a manual save. */
    private fun autoSave() {
        val now = System.currentTimeMillis()
        tabs.tabs.forEach { tab ->
            val doc = documents[tab.id] ?: return@forEach
            val text = doc.state.text.toString()
            val due = AutoSave.isDue(settings.autoSave, tab.uri != null, doc.isDirty(tab), lastEditAt, now)
            if (!due || tab.id in saving || !TextCodec.canEncode(text, tab.encoding)) return@forEach
            write(tab, Uri.parse(tab.uri), text, quiet = true)
        }
    }

    private fun rememberRecent(uri: Uri, name: String) {
        updateRecent(RecentFiles.add(recent, RecentFile(uri.toString(), name, System.currentTimeMillis())))
    }

    private fun updateRecent(list: List<RecentFile>) {
        recent = list
        settingsStore.saveRecent(list)
    }

    private fun message(@StringRes text: Int, vararg args: Any) {
        messages.trySend(UiMessage(text, args.toList()))
    }

    private fun string(@StringRes id: Int): String = getApplication<Application>().getString(id)

    private companion object {
        const val DRAFT_DELAY_MILLIS = 1_000L

        /** Outlives the view model: drafts written as the window goes away must still land. */
        val DRAFT_WRITER = CoroutineScope(Dispatchers.IO)
    }
}
