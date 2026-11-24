package org.lineageos.pctexteditor.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FileTypesTest {
    @Test
    fun mimeForKnownExtensions() {
        assertEquals("text/plain", FileTypes.mimeFor("notas.txt"))
        assertEquals("text/markdown", FileTypes.mimeFor("LEIAME.MD"))
        assertEquals("application/json", FileTypes.mimeFor("config.json"))
        assertEquals("text/x-kotlin", FileTypes.mimeFor("Main.kt"))
    }

    @Test
    fun unknownOrMissingExtensionIsPlainText() {
        assertEquals("text/plain", FileTypes.mimeFor("Makefile"))
        assertEquals("text/plain", FileTypes.mimeFor("arquivo.xyz"))
    }

    @Test
    fun saveNameAddsTxtOnlyWhenThereIsNoExtension() {
        assertEquals("Untitled.txt", FileTypes.saveNameFor("Untitled"))
        assertEquals("script.sh", FileTypes.saveNameFor("script.sh"))
        assertEquals(".bashrc", FileTypes.saveNameFor(".bashrc"))
    }
}
