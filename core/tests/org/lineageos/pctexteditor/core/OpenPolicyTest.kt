package org.lineageos.pctexteditor.core

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenPolicyTest {
    private val text = "olá".toByteArray()

    @Test
    fun smallTextOpens() {
        assertEquals(OpenDecision.OPEN, OpenPolicy.decide(size = 3, head = text))
    }

    @Test
    fun largeTextAsksFirst() {
        assertEquals(OpenDecision.CONFIRM_LARGE, OpenPolicy.decide(size = OpenPolicy.LARGE_BYTES + 1, head = text))
    }

    @Test
    fun hugeFilesAreRefused() {
        assertEquals(OpenDecision.TOO_LARGE, OpenPolicy.decide(size = OpenPolicy.MAX_BYTES + 1, head = text))
    }

    @Test
    fun binaryIsRefusedWhateverTheSize() {
        val png = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 0, 0, 0, 1)
        assertEquals(OpenDecision.NOT_TEXT, OpenPolicy.decide(size = 6, head = png))
    }

    @Test
    fun unknownSizeIsTreatedAsSmall() {
        assertEquals(OpenDecision.OPEN, OpenPolicy.decide(size = -1, head = text))
    }
}
