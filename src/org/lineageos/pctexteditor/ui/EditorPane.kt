package org.lineageos.pctexteditor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.lineageos.pctexteditor.EditorSettings
import org.lineageos.pctexteditor.FindUi
import org.lineageos.pctexteditor.R
import org.lineageos.pctexteditor.TabDocument
import org.lineageos.pctexteditor.core.TextLines

private val TEXT_PADDING_H = 12.dp
private val TEXT_PADDING_V = 8.dp
private val GUTTER_PADDING = 12.dp
private const val LINE_HEIGHT = 1.5f

/** The text with an optional line number gutter. Without word wrap the text scrolls sideways. */
@Composable
fun EditorPane(doc: TabDocument, settings: EditorSettings, find: FindUi, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    var layout by remember(doc) { mutableStateOf<TextLayoutResult?>(null) }
    val style = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = settings.fontSize.sp,
        lineHeight = (settings.fontSize * LINE_HEIGHT).sp,
        color = colors.onSurface,
    )
    val highlights = MatchColors(all = colors.primary.copy(alpha = 0.16f), current = colors.tertiary.copy(alpha = 0.45f))
    val placeholder = stringResource(R.string.editor_placeholder)

    BoxWithConstraints(modifier.fillMaxSize().background(colors.surface)) {
        val viewportHeight = constraints.maxHeight
        val viewportWidth = maxWidth
        ScrollToMatch(find, layout, vertical, horizontal, viewportHeight)
        Row(Modifier.fillMaxSize().verticalScroll(vertical)) {
            if (settings.lineNumbers) LineGutter(doc.state.text, layout, style, vertical, viewportHeight)
            val sideways = Modifier.horizontalScroll(horizontal).widthIn(min = viewportWidth).takeUnless {
                settings.wordWrap
            } ?: Modifier
            Box(Modifier.weight(1f).then(sideways)) {
                BasicTextField(
                    state = doc.state,
                    modifier = Modifier.padding(horizontal = TEXT_PADDING_H, vertical = TEXT_PADDING_V)
                        .drawBehind { drawMatches(layout, find, highlights) },
                    textStyle = style,
                    cursorBrush = SolidColor(colors.primary),
                    onTextLayout = { result -> layout = result() },
                    decorator = placeholderDecorator(doc, placeholder, style.copy(color = colors.onSurfaceVariant)),
                )
            }
        }
    }
}

private fun placeholderDecorator(doc: TabDocument, placeholder: String, style: TextStyle) =
    TextFieldDecorator { inner ->
        Box {
            if (doc.state.text.isEmpty()) Text(placeholder, style = style)
            inner()
        }
    }

private data class MatchColors(val all: Color, val current: Color)

private fun DrawScope.drawMatches(layout: TextLayoutResult?, find: FindUi, colors: MatchColors) {
    if (layout == null || !find.visible) return
    val length = layout.layoutInput.text.length
    val current = find.result.currentMatch
    find.result.matches.filter { it.last < length }.forEach { match ->
        val color = colors.current.takeIf { match == current } ?: colors.all
        drawPath(layout.getPathForRange(match.first, match.last + 1), color)
    }
}

/** Brings the selected match into view when it changes. */
@Composable
private fun ScrollToMatch(
    find: FindUi,
    layout: TextLayoutResult?,
    vertical: ScrollState,
    horizontal: ScrollState,
    viewportHeight: Int,
) {
    val match = find.result.currentMatch
    LaunchedEffect(match) {
        val result = layout ?: return@LaunchedEffect
        val offset = match?.first?.takeIf { it <= result.layoutInput.text.length } ?: return@LaunchedEffect
        val top = result.getLineTop(result.getLineForOffset(offset)).toInt()
        val isVisible = top >= vertical.value && top < vertical.value + viewportHeight * 3 / 4
        if (!isVisible) vertical.animateScrollTo(top - viewportHeight / 3)
        horizontal.animateScrollTo((result.getHorizontalPosition(offset, true) - 200).toInt().coerceAtLeast(0))
    }
}

/** Line numbers aligned with the first visual line of each text line; only the visible ones are drawn. */
@Composable
private fun LineGutter(
    text: CharSequence,
    layout: TextLayoutResult?,
    style: TextStyle,
    scroll: ScrollState,
    viewportHeight: Int,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val starts = remember(text) { TextLines.lineStarts(text) }
    val numberStyle = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val digitWidth = remember(numberStyle) { measurer.measure("0", numberStyle).size.width }
    val width = with(density) { (digitWidth * TextLines.digits(starts.size)).toDp() } + GUTTER_PADDING * 2
    val height = with(density) { (layout?.size?.height ?: 0).toDp() } + TEXT_PADDING_V * 2
    Canvas(Modifier.width(width).height(height).background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        val result = layout ?: return@Canvas
        val window = scroll.value.toFloat()..(scroll.value + viewportHeight).toFloat()
        drawLineNumbers(measurer, result, starts, numberStyle, window, TEXT_PADDING_V.toPx(), GUTTER_PADDING.toPx())
    }
}

private fun DrawScope.drawLineNumbers(
    measurer: TextMeasurer,
    layout: TextLayoutResult,
    starts: IntArray,
    style: TextStyle,
    window: ClosedFloatingPointRange<Float>,
    top: Float,
    end: Float,
) {
    val length = layout.layoutInput.text.length
    val lineTop = { index: Int -> top + layout.getLineTop(layout.getLineForOffset(starts[index].coerceAtMost(length))) }
    val firstVisual = layout.getLineForVerticalPosition((window.start - top).coerceAtLeast(0f))
    val found = starts.binarySearch(layout.getLineStart(firstVisual))
    // Not found: the insertion point minus one is the text line that holds that visual line.
    val first = found.takeIf { it >= 0 } ?: (-found - 2)
    for (index in first.coerceAtLeast(0) until starts.size) {
        val y = lineTop(index)
        if (y > window.endInclusive) return
        val number = measurer.measure((index + 1).toString(), style)
        drawText(number, topLeft = Offset(size.width - end - number.size.width, y))
    }
}
