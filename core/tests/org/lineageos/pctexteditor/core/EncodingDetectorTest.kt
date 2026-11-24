package org.lineageos.pctexteditor.core

import org.junit.Assert.assertEquals
import org.junit.Test

class EncodingDetectorTest {
    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun emptyFileIsUtf8() {
        assertEquals(TextEncoding.UTF_8, EncodingDetector.detect(ByteArray(0)))
    }

    @Test
    fun plainAsciiIsUtf8() {
        assertEquals(TextEncoding.UTF_8, EncodingDetector.detect("hello\n".toByteArray()))
    }

    @Test
    fun validUtf8WithAccentsIsUtf8() {
        assertEquals(TextEncoding.UTF_8, EncodingDetector.detect("ação é útil".toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun utf8BomWins() {
        assertEquals(TextEncoding.UTF_8_BOM, EncodingDetector.detect(bytes(0xEF, 0xBB, 0xBF, 'a'.code)))
    }

    @Test
    fun utf16LittleEndianBom() {
        assertEquals(TextEncoding.UTF_16LE, EncodingDetector.detect(bytes(0xFF, 0xFE, 'a'.code, 0)))
    }

    @Test
    fun utf16BigEndianBom() {
        assertEquals(TextEncoding.UTF_16BE, EncodingDetector.detect(bytes(0xFE, 0xFF, 0, 'a'.code)))
    }

    @Test
    fun utf16WithoutBomIsRecognisedByItsZeroBytes() {
        assertEquals(TextEncoding.UTF_16LE, EncodingDetector.detect("texto simples".toByteArray(Charsets.UTF_16LE)))
        assertEquals(TextEncoding.UTF_16BE, EncodingDetector.detect("texto simples".toByteArray(Charsets.UTF_16BE)))
    }

    @Test
    fun invalidUtf8FallsBackToWindows1252() {
        val latin = "ação".toByteArray(charset("windows-1252"))
        assertEquals(TextEncoding.WINDOWS_1252, EncodingDetector.detect(latin))
    }

    @Test
    fun utf8CutInTheMiddleOfACharacterIsStillUtf8() {
        val full = "ação".toByteArray(Charsets.UTF_8)
        assertEquals(TextEncoding.UTF_8, EncodingDetector.detect(full.copyOf(full.size - 1), truncated = true))
    }

    @Test
    fun binaryHasZeroBytesOutsideUtf16() {
        assertEquals(true, EncodingDetector.isBinary(bytes(0x89, 'P'.code, 'N'.code, 'G'.code, 0, 0, 0, 0x0D, 1, 2)))
        assertEquals(false, EncodingDetector.isBinary("texto".toByteArray()))
        assertEquals(false, EncodingDetector.isBinary("texto".toByteArray(Charsets.UTF_16LE)))
    }
}
