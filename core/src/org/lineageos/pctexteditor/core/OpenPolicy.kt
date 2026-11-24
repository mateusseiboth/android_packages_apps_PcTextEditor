package org.lineageos.pctexteditor.core

enum class OpenDecision { OPEN, CONFIRM_LARGE, TOO_LARGE, NOT_TEXT }

/** Whether a file can be opened, from its size (-1 when unknown) and its first bytes. */
object OpenPolicy {
    const val HEAD_BYTES = 8 * 1024
    const val LARGE_BYTES = 2L shl 20
    const val MAX_BYTES = 32L shl 20

    private val RULES: List<Pair<(Long, ByteArray) -> Boolean, OpenDecision>> = listOf(
        { _: Long, head: ByteArray -> EncodingDetector.isBinary(head) } to OpenDecision.NOT_TEXT,
        { size: Long, _: ByteArray -> size > MAX_BYTES } to OpenDecision.TOO_LARGE,
        { size: Long, _: ByteArray -> size > LARGE_BYTES } to OpenDecision.CONFIRM_LARGE,
    )

    fun decide(size: Long, head: ByteArray): OpenDecision =
        RULES.firstOrNull { (applies, _) -> applies(size, head) }?.second ?: OpenDecision.OPEN
}
