package org.lineageos.pctexteditor.core

data class DecodedText(val text: String, val encoding: TextEncoding, val lineEnding: LineEnding)

/** File bytes to editor text and back, keeping the file's encoding, BOM and line break. */
object TextCodec {
    fun decode(bytes: ByteArray): DecodedText = decode(bytes, EncodingDetector.detect(bytes))

    fun decode(bytes: ByteArray, encoding: TextEncoding): DecodedText {
        val skip = encoding.bom.size.takeIf { encoding.hasBomAt(bytes) } ?: 0
        val raw = String(bytes, skip, bytes.size - skip, encoding.charset)
        return DecodedText(LineEndings.normalize(raw), encoding, LineEndings.detect(raw))
    }

    fun encode(text: String, encoding: TextEncoding, lineEnding: LineEnding): ByteArray =
        encoding.bom + LineEndings.apply(text, lineEnding).toByteArray(encoding.charset)

    /** False when saving [text] in [encoding] would replace some characters. */
    fun canEncode(text: String, encoding: TextEncoding): Boolean = encoding.charset.newEncoder().canEncode(text)
}
