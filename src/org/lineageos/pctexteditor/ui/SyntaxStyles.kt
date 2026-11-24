package org.lineageos.pctexteditor.ui

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.lineageos.pctexteditor.TabDocument
import org.lineageos.pctexteditor.core.TokenType

private fun palette(vararg colors: Pair<TokenType, Long>) = colors.associate { (type, argb) -> type to Color(argb) }

private val LIGHT = palette(
    TokenType.KEYWORD to 0xFF8E24AA, TokenType.TYPE to 0xFF00796B, TokenType.STRING to 0xFF2E7D32,
    TokenType.NUMBER to 0xFF1565C0, TokenType.COMMENT to 0xFF757575, TokenType.META to 0xFFAD1457,
    TokenType.TAG to 0xFF1565C0, TokenType.ATTRIBUTE to 0xFF8D6E00, TokenType.PROPERTY to 0xFF0277BD,
    TokenType.HEADING to 0xFF1565C0, TokenType.EMPHASIS to 0xFF6A1B9A, TokenType.CODE to 0xFF5D4037,
    TokenType.LINK to 0xFF0277BD, TokenType.VARIABLE to 0xFFC62828, TokenType.CONSTANT to 0xFFE65100,
)

private val DARK = palette(
    TokenType.KEYWORD to 0xFFCE93D8, TokenType.TYPE to 0xFF80CBC4, TokenType.STRING to 0xFFA5D6A7,
    TokenType.NUMBER to 0xFF90CAF9, TokenType.COMMENT to 0xFF9E9E9E, TokenType.META to 0xFFF48FB1,
    TokenType.TAG to 0xFF90CAF9, TokenType.ATTRIBUTE to 0xFFFFCC80, TokenType.PROPERTY to 0xFF81D4FA,
    TokenType.HEADING to 0xFF90CAF9, TokenType.EMPHASIS to 0xFFE1BEE7, TokenType.CODE to 0xFFBCAAA4,
    TokenType.LINK to 0xFF81D4FA, TokenType.VARIABLE to 0xFFEF9A9A, TokenType.CONSTANT to 0xFFFFB74D,
)

/** Extra weight, slant or underline on top of the colour. */
private val SHAPES = mapOf(
    TokenType.COMMENT to SpanStyle(fontStyle = FontStyle.Italic),
    TokenType.EMPHASIS to SpanStyle(fontStyle = FontStyle.Italic),
    TokenType.HEADING to SpanStyle(fontWeight = FontWeight.Bold),
    TokenType.LINK to SpanStyle(textDecoration = TextDecoration.Underline),
)

/** The span style of each token type, with the palette that suits the current theme's background. */
@Composable
fun syntaxStyles(): Map<TokenType, SpanStyle> {
    val colors = DARK.takeIf { MaterialTheme.colorScheme.surface.luminance() < 0.5f } ?: LIGHT
    return TokenType.entries.associateWith { type ->
        SpanStyle(color = colors.getValue(type)).merge(SHAPES[type])
    }
}

/**
 * Colours the shown text with the tab's tokens. The tokens are computed after typing pauses, so they may
 * briefly lag the text; spans past the end are skipped.
 */
class SyntaxColoring(private val doc: TabDocument, private val styles: Map<TokenType, SpanStyle>) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        val end = length
        doc.tokens.filter { it.end <= end }.forEach { addStyle(styles.getValue(it.type), it.start, it.end) }
    }
}
