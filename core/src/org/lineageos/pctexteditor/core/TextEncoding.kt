package org.lineageos.pctexteditor.core

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/** The encodings the editor reads and writes. [bom] is written before the text when saving. */
enum class TextEncoding(val charsetName: String, val label: String, bomBytes: IntArray = IntArray(0)) {
    UTF_8("UTF-8", "UTF-8"),
    UTF_8_BOM("UTF-8", "UTF-8 BOM", intArrayOf(0xEF, 0xBB, 0xBF)),
    UTF_16LE("UTF-16LE", "UTF-16 LE", intArrayOf(0xFF, 0xFE)),
    UTF_16BE("UTF-16BE", "UTF-16 BE", intArrayOf(0xFE, 0xFF)),
    WINDOWS_1252("windows-1252", "Windows-1252"),
    ISO_8859_1("ISO-8859-1", "ISO-8859-1");

    val bom: ByteArray = ByteArray(bomBytes.size) { bomBytes[it].toByte() }
    val charset: Charset get() = Charset.forName(charsetName)

    fun hasBomAt(bytes: ByteArray): Boolean =
        bom.isNotEmpty() && bytes.size >= bom.size && bom.indices.all { bytes[it] == bom[it] }
}

/** Guesses how a file's bytes encode text: a BOM first, then UTF-16 by its zero bytes, then UTF-8, else Windows-1252. */
object EncodingDetector {
    private val WITH_BOM = TextEncoding.entries.filter { it.bom.isNotEmpty() }
    private const val UTF16_ZERO_SHARE = 0.3

    /** [truncated] says [bytes] is only the head of the file, so a character cut at the end is not an error. */
    fun detect(bytes: ByteArray, truncated: Boolean = false): TextEncoding {
        WITH_BOM.firstOrNull { it.hasBomAt(bytes) }?.let { return it }
        withoutBomUtf16(bytes)?.let { return it }
        return TextEncoding.UTF_8.takeIf { isUtf8(bytes, truncated) } ?: TextEncoding.WINDOWS_1252
    }

    /** Zero bytes in text that is neither UTF-16 nor marked by a BOM mean the file is not text. */
    fun isBinary(bytes: ByteArray): Boolean {
        val encoding = detect(bytes, truncated = true)
        val isUtf16 = encoding == TextEncoding.UTF_16LE || encoding == TextEncoding.UTF_16BE
        return !isUtf16 && bytes.any { it.toInt() == 0 }
    }

    private fun withoutBomUtf16(bytes: ByteArray): TextEncoding? {
        if (bytes.size < 2) return null
        val pairs = bytes.size / 2
        val zerosAtOdd = (0 until pairs).count { bytes[it * 2 + 1].toInt() == 0 }
        val zerosAtEven = (0 until pairs).count { bytes[it * 2].toInt() == 0 }
        val threshold = pairs * UTF16_ZERO_SHARE
        return when {
            zerosAtOdd > threshold && zerosAtEven == 0 -> TextEncoding.UTF_16LE
            zerosAtEven > threshold && zerosAtOdd == 0 -> TextEncoding.UTF_16BE
            else -> null
        }
    }

    private fun isUtf8(bytes: ByteArray, truncated: Boolean): Boolean {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val input = ByteBuffer.wrap(bytes)
        val output = java.nio.CharBuffer.allocate(bytes.size + 1)
        val result = decoder.decode(input, output, !truncated)
        return !result.isError
    }
}
