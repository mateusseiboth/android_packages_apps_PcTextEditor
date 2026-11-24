package org.lineageos.pctexteditor.core

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentFilesTest {
    private fun file(n: Int, at: Long = n.toLong()) = RecentFile("content://doc/$n", "nota $n.txt", at)

    @Test
    fun addPutsTheFileFirst() {
        val list = RecentFiles.add(listOf(file(1), file(2)), file(3))
        assertEquals(listOf(3, 1, 2).map { "content://doc/$it" }, list.map { it.uri })
    }

    @Test
    fun addMovesAFileAlreadyListedToTheTop() {
        val list = RecentFiles.add(listOf(file(1), file(2)), file(2, at = 99))
        assertEquals(listOf("content://doc/2", "content://doc/1"), list.map { it.uri })
        assertEquals(99L, list.first().openedAt)
    }

    @Test
    fun addKeepsAtMostTheLimit() {
        val list = (1..30).fold(emptyList<RecentFile>()) { acc, n -> RecentFiles.add(acc, file(n)) }
        assertEquals(RecentFiles.LIMIT, list.size)
        assertEquals("content://doc/30", list.first().uri)
    }

    @Test
    fun removeDropsTheFile() {
        assertEquals(listOf(file(2)), RecentFiles.remove(listOf(file(1), file(2)), "content://doc/1"))
    }

    @Test
    fun formatAndParseRoundTrip() {
        val list = listOf(file(1), RecentFile("content://x/a%20b", "tab\tand\nnewline", 5))
        assertEquals(list, RecentFiles.parse(RecentFiles.format(list)))
    }

    @Test
    fun parseSkipsBrokenLines() {
        assertEquals(listOf(file(1)), RecentFiles.parse("garbage\n" + RecentFiles.format(listOf(file(1)))))
    }

    @Test
    fun dayLabels() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val now = Instant.parse("2026-11-24T15:00:00Z").toEpochMilli()
        val today = Instant.parse("2026-11-24T04:00:00Z").toEpochMilli()
        val yesterday = Instant.parse("2026-11-23T20:00:00Z").toEpochMilli()
        val older = Instant.parse("2026-11-20T20:00:00Z").toEpochMilli()
        assertEquals(RecentDay.TODAY, RecentFiles.dayOf(today, now, zone))
        assertEquals(RecentDay.YESTERDAY, RecentFiles.dayOf(yesterday, now, zone))
        assertEquals(RecentDay.EARLIER, RecentFiles.dayOf(older, now, zone))
    }
}
