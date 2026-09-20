package com.macrobite.app.ui.chat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object MarkdownTextFormatter {

    fun format(raw: String, highlightColor: Color = Color.Unspecified): AnnotatedString {
        if (raw.isBlank()) return AnnotatedString("")

        return buildAnnotatedString {
            val lines = raw.lines()
            lines.forEachIndexed { lineIdx, line ->
                var trimmedLine = line.trimEnd()

                // Replace markdown bullets like "* " or "- " with clean bullet point
                if (trimmedLine.startsWith("* ") || trimmedLine.startsWith("- ")) {
                    append("  • ")
                    trimmedLine = trimmedLine.substring(2)
                }

                // Match ***bold-italic***, **bold**, *italic*
                val pattern = Regex("""(\*\*\*(.+?)\*\*\*|\*\*(.+?)\*\*|\*(.+?)\*)""")
                var lastIndex = 0
                val matches = pattern.findAll(trimmedLine)

                for (match in matches) {
                    if (match.range.first > lastIndex) {
                        append(cleanStrayAsterisks(trimmedLine.substring(lastIndex, match.range.first)))
                    }

                    val fullMatch = match.value
                    when {
                        fullMatch.startsWith("***") && fullMatch.endsWith("***") -> {
                            val content = match.groupValues[2]
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = highlightColor)) {
                                append(content)
                            }
                        }
                        fullMatch.startsWith("**") && fullMatch.endsWith("**") -> {
                            val content = match.groupValues[3]
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = highlightColor)) {
                                append(content)
                            }
                        }
                        fullMatch.startsWith("*") && fullMatch.endsWith("*") -> {
                            val content = match.groupValues[4]
                            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                                append(content)
                            }
                        }
                        else -> append(fullMatch)
                    }
                    lastIndex = match.range.last + 1
                }

                if (lastIndex < trimmedLine.length) {
                    append(cleanStrayAsterisks(trimmedLine.substring(lastIndex)))
                }

                if (lineIdx < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }

    private fun cleanStrayAsterisks(text: String): String {
        return text.replace("***", "").replace("**", "")
    }
}
