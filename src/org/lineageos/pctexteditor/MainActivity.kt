package org.lineageos.pctexteditor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import org.lineageos.pctexteditor.ui.EditorScreen
import org.lineageos.pctexteditor.ui.TextEditorTheme

class MainActivity : ComponentActivity() {
    private val vm: EditorViewModel by viewModels()

    /** What each incoming action does: files open in a tab, shared text becomes a new tab. */
    private val intentHandlers: Map<String, (Intent) -> Unit> = mapOf(
        Intent.ACTION_VIEW to ::openData,
        Intent.ACTION_EDIT to ::openData,
        Intent.ACTION_SEND to ::openSharedText,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        savedInstanceState ?: handle(intent)
        setContent {
            TextEditorTheme(vm.settings.theme) { EditorScreen(vm) }
        }
    }

    /** Unsaved work is kept as drafts whenever the window leaves the screen. */
    override fun onStop() {
        super.onStop()
        vm.saveDraftsNow()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        val action = intent?.action ?: return
        intentHandlers[action]?.invoke(intent)
    }

    private fun openData(intent: Intent) {
        intent.data?.let { uri: Uri -> vm.openUri(uri) }
    }

    private fun openSharedText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { vm.newTab(it) }
    }
}
