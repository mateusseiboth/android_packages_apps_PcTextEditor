package org.lineageos.pctexteditor.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ChangeCheckTest {
    private val known = FileStamp(modified = 100, size = 10, hash = null)

    @Test
    fun sameStampIsNoChange() {
        assertEquals(ExternalChange.NONE, ChangeCheck.decide(known, known.copy(), isDirty = true))
    }

    @Test
    fun changedFileReloadsACleanTab() {
        assertEquals(ExternalChange.RELOAD, ChangeCheck.decide(known, known.copy(modified = 200), isDirty = false))
    }

    @Test
    fun changedFileAsksWhenTheTabHasChanges() {
        assertEquals(ExternalChange.ASK, ChangeCheck.decide(known, known.copy(size = 11), isDirty = true))
    }

    @Test
    fun onlyFieldsKnownOnBothSidesAreCompared() {
        val noDate = FileStamp(modified = null, size = 10, hash = 5)
        assertEquals(ExternalChange.NONE, ChangeCheck.decide(known, noDate, isDirty = false))
        assertEquals(ExternalChange.RELOAD, ChangeCheck.decide(noDate, noDate.copy(hash = 6), isDirty = false))
    }

    @Test
    fun unreachableFileIsGone() {
        assertEquals(ExternalChange.GONE, ChangeCheck.decide(known, null, isDirty = false))
    }

    @Test
    fun withoutAKnownStampThereIsNothingToCompare() {
        assertEquals(ExternalChange.NONE, ChangeCheck.decide(null, known, isDirty = false))
        assertEquals(ExternalChange.NONE, ChangeCheck.decide(null, null, isDirty = false))
    }
}
