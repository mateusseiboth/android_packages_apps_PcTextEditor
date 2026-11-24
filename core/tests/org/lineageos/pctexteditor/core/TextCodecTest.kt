package org.lineageos.pctexteditor.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextCodecTest {
    @Test
    fun decodeStripsTheBomAndNormalizesBreaks() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "olá\r\nmundo".toByteArray()
        val decoded = TextCodec.decode(bytes)
        assertEquals("olá\nmundo", decoded.text)
        assertEquals(TextEncoding.UTF_8_BOM, decoded.encoding)
        assertEquals(LineEnding.CRLF, decoded.lineEnding)
    }

    @Test
    fun decodeWindows1252() {
        val decoded = TextCodec.decode("ação\n".toByteArray(charset("windows-1252")))
        assertEquals("ação\n", decoded.text)
        assertEquals(TextEncoding.WINDOWS_1252, decoded.encoding)
    }

    @Test
    fun decodeWithAChosenEncoding() {
        val bytes = "ação".toByteArray(Charsets.UTF_8)
        assertEquals("aÃ§Ã£o", TextCodec.decode(bytes, TextEncoding.WINDOWS_1252).text)
    }

    @Test
    fun decodeUtf16LittleEndianWithBom() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "oi\n".toByteArray(Charsets.UTF_16LE)
        assertEquals("oi\n", TextCodec.decode(bytes).text)
    }

    @Test
    fun encodeRoundTripsEncodingBomAndBreaks() {
        val original = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "a\r\nb\r\n".toByteArray()
        val decoded = TextCodec.decode(original)
        assertArrayEquals(original, TextCodec.encode(decoded.text, decoded.encoding, decoded.lineEnding))
    }

    @Test
    fun encodeUtf16BigEndianWritesItsBom() {
        val bytes = TextCodec.encode("a", TextEncoding.UTF_16BE, LineEnding.LF)
        assertArrayEquals(byteArrayOf(0xFE.toByte(), 0xFF.toByte(), 0, 'a'.code.toByte()), bytes)
    }

    @Test
    fun utf16WithoutBomIsSavedWithoutBom() {
        val original = "a\r\nb".toByteArray(Charsets.UTF_16LE)
        val decoded = TextCodec.decode(original)
        assertEquals("a\nb", decoded.text)
        assertArrayEquals(original, TextCodec.encode(decoded.text, decoded.encoding, decoded.lineEnding))
    }

    @Test
    fun canEncodeTellsWhenCharactersWouldBeLost() {
        assertTrue(TextCodec.canEncode("ação", TextEncoding.WINDOWS_1252))
        assertFalse(TextCodec.canEncode("ação 😀", TextEncoding.WINDOWS_1252))
        assertTrue(TextCodec.canEncode("ação 😀", TextEncoding.UTF_8))
    }
}
