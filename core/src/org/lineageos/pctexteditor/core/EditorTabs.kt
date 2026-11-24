package org.lineageos.pctexteditor.core

/** A tab's document: [uri] is null until an untitled tab is saved. */
data class TabInfo(
    val id: Long,
    val name: String,
    val uri: String?,
    val encoding: TextEncoding = TextEncoding.UTF_8,
    val lineEnding: LineEnding = LineEnding.LF,
)

/** The open tabs, in order, and the selected one. Every operation returns a new value. */
data class EditorTabs(val tabs: List<TabInfo>, val selectedId: Long?) {
    val selected: TabInfo? get() = tabs.firstOrNull { it.id == selectedId }

    /** A file already open is selected instead of opened twice. */
    fun open(tab: TabInfo): EditorTabs {
        val existing = tab.uri?.let { uri -> tabs.firstOrNull { it.uri == uri } }
        existing?.let { return select(it.id) }
        return EditorTabs(tabs + tab, tab.id)
    }

    fun select(id: Long): EditorTabs = copy(selectedId = id.takeIf { has(it) } ?: selectedId)

    /** Closing the selected tab selects its right neighbour, or the left one at the end. */
    fun close(id: Long): EditorTabs {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return this
        val remaining = tabs.filterNot { it.id == id }
        val keep = selectedId.takeIf { it != id }
        val neighbour = remaining.getOrNull(index.coerceAtMost(remaining.lastIndex))?.id
        return EditorTabs(remaining, keep ?: neighbour)
    }

    fun update(id: Long, transform: (TabInfo) -> TabInfo): EditorTabs =
        copy(tabs = tabs.map { tab -> tab.takeUnless { it.id == id } ?: transform(tab) })

    fun selectNext(): EditorTabs = selectBy(1)

    fun selectPrevious(): EditorTabs = selectBy(-1)

    private fun selectBy(step: Int): EditorTabs {
        val index = tabs.indexOfFirst { it.id == selectedId }
        if (index < 0) return this
        return select(tabs[Math.floorMod(index + step, tabs.size)].id)
    }

    private fun has(id: Long) = tabs.any { it.id == id }

    companion object {
        val EMPTY = EditorTabs(emptyList(), null)
    }
}

object UntitledNames {
    /** "Untitled", then "Untitled 2", "Untitled 3"... the lowest number not in use. */
    fun next(names: Collection<String>, base: String): String =
        generateSequence(1) { it + 1 }
            .map { n -> base.takeIf { n == 1 } ?: "$base $n" }
            .first { it !in names }
}
