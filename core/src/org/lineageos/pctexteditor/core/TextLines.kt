package org.lineageos.pctexteditor.core

data class LineColumn(val line: Int, val column: Int)

/** Line arithmetic over editor text (breaks are always "\n"). */
object TextLines {
    fun lineStarts(text: CharSequence): IntArray {
        val starts = ArrayList<Int>().apply { add(0) }
        text.forEachIndexed { i, c -> if (c == '\n') starts.add(i + 1) }
        return starts.toIntArray()
    }

    fun positionOf(text: CharSequence, offset: Int): LineColumn = positionOf(lineStarts(text), text.length, offset)

    fun positionOf(starts: IntArray, length: Int, offset: Int): LineColumn {
        val at = offset.coerceIn(0, length)
        val line = starts.indexOfLast { it <= at }
        return LineColumn(line + 1, at - starts[line] + 1)
    }

    fun digits(lineCount: Int): Int = lineCount.coerceAtLeast(1).toString().length
}
