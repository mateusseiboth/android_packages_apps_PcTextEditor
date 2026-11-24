package org.lineageos.pctexteditor.core

import java.net.URLDecoder
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class RecentFile(val uri: String, val name: String, val openedAt: Long)

enum class RecentDay { TODAY, YESTERDAY, EARLIER }

/** The recent files list (newest first) and its one-line-per-file storage format. */
object RecentFiles {
    const val LIMIT = 20
    private const val FIELDS = 3

    fun add(list: List<RecentFile>, file: RecentFile): List<RecentFile> =
        (listOf(file) + remove(list, file.uri)).take(LIMIT)

    fun remove(list: List<RecentFile>, uri: String): List<RecentFile> = list.filterNot { it.uri == uri }

    fun format(list: List<RecentFile>): String =
        list.joinToString("\n") { listOf(it.uri, it.name, it.openedAt.toString()).joinToString(" ") { f -> encode(f) } }

    fun parse(stored: String): List<RecentFile> = stored.lines().mapNotNull { parseLine(it) }

    fun dayOf(openedAt: Long, now: Long, zone: ZoneId): RecentDay {
        val day = dateOf(openedAt, zone)
        val today = dateOf(now, zone)
        return mapOf(today to RecentDay.TODAY, today.minusDays(1) to RecentDay.YESTERDAY)[day] ?: RecentDay.EARLIER
    }

    private fun dateOf(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    private fun parseLine(line: String): RecentFile? {
        val fields = line.split(" ").takeIf { it.size == FIELDS }?.map { decode(it) } ?: return null
        val openedAt = fields[2].toLongOrNull() ?: return null
        return RecentFile(fields[0], fields[1], openedAt)
    }

    private fun encode(field: String) = URLEncoder.encode(field, Charsets.UTF_8.name())

    private fun decode(field: String) = URLDecoder.decode(field, Charsets.UTF_8.name())
}
