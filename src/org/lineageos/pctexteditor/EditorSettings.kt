package org.lineageos.pctexteditor

import android.content.SharedPreferences
import org.lineageos.pctexteditor.core.RecentFile
import org.lineageos.pctexteditor.core.RecentFiles

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class EditorSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: Int = 14,
    val lineNumbers: Boolean = true,
    val wordWrap: Boolean = true,
    val sidebarOnStart: Boolean = true,
) {
    companion object {
        val FONT_SIZES = 10..28
    }
}

/** Settings and the recent files list, kept in the app's preferences. */
class SettingsStore(private val prefs: SharedPreferences) {
    fun readSettings(): EditorSettings {
        val defaults = EditorSettings()
        val theme = ThemeMode.entries.firstOrNull { it.name == prefs.getString(THEME, null) } ?: defaults.theme
        return EditorSettings(
            theme = theme,
            fontSize = prefs.getInt(FONT_SIZE, defaults.fontSize).coerceIn(EditorSettings.FONT_SIZES),
            lineNumbers = prefs.getBoolean(LINE_NUMBERS, defaults.lineNumbers),
            wordWrap = prefs.getBoolean(WORD_WRAP, defaults.wordWrap),
            sidebarOnStart = prefs.getBoolean(SIDEBAR, defaults.sidebarOnStart),
        )
    }

    fun saveSettings(settings: EditorSettings) {
        prefs.edit()
            .putString(THEME, settings.theme.name)
            .putInt(FONT_SIZE, settings.fontSize)
            .putBoolean(LINE_NUMBERS, settings.lineNumbers)
            .putBoolean(WORD_WRAP, settings.wordWrap)
            .putBoolean(SIDEBAR, settings.sidebarOnStart)
            .apply()
    }

    fun readRecent(): List<RecentFile> = RecentFiles.parse(prefs.getString(RECENT, "") ?: "")

    fun saveRecent(list: List<RecentFile>) {
        prefs.edit().putString(RECENT, RecentFiles.format(list)).apply()
    }

    private companion object {
        const val THEME = "theme"
        const val FONT_SIZE = "font_size"
        const val LINE_NUMBERS = "line_numbers"
        const val WORD_WRAP = "word_wrap"
        const val SIDEBAR = "sidebar_on_start"
        const val RECENT = "recent_files"
    }
}
