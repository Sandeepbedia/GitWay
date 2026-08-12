/*
 * GitWay — an Android client for GitHub.
 *
 * This file is part of GitWay. GitWay is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * GitWay is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GitWay. If not, see <https://www.gnu.org/licenses/>.
 */
package com.io.git.way.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Minimal regex-based syntax highlighter for the Repository Browser's file viewer/editor
 * (not a real tokenizer — good enough to make code readable at a glance: keywords,
 * strings, comments, numbers, without pulling in a full highlighting library/dependency).
 */
object SyntaxHighlighter {

    private val KEYWORDS = setOf(
        "fun", "val", "var", "class", "object", "interface", "if", "else", "when", "for", "while",
        "return", "import", "package", "private", "public", "protected", "internal", "override",
        "companion", "suspend", "true", "false", "null", "this", "super", "is", "as", "in", "try",
        "catch", "finally", "throw", "new", "extends", "implements", "static", "final", "void",
        "const", "let", "function", "def", "from", "export", "default", "async", "await",
        "struct", "enum", "case", "switch", "break", "continue", "do", "namespace", "type", "int",
        "string", "bool", "float", "double", "long", "self"
    )

    private val STRING_REGEX = Regex("\"(?:\\\\.|[^\"\\\\\n])*\"|'(?:\\\\.|[^'\\\\\n])*'")
    private val COMMENT_REGEX = Regex("//[^\n]*|#[^\n]*")
    private val NUMBER_REGEX = Regex("\\b\\d+(\\.\\d+)?[fFlLdD]?\\b")
    private val IDENTIFIER_REGEX = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")

    private val keywordColor = Color(0xFFCF6EE4)
    private val stringColor = Color(0xFF9ECE6A)
    private val commentColor = Color(0xFF7C818C)
    private val numberColor = Color(0xFFE0AF68)

    fun highlight(code: String): AnnotatedString = buildAnnotatedString {
        append(code)
        if (code.isEmpty()) return@buildAnnotatedString

        val consumed = BooleanArray(code.length)

        fun markConsumed(range: IntRange) {
            for (i in range) if (i in code.indices) consumed[i] = true
        }

        COMMENT_REGEX.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = commentColor), match.range.first, match.range.last + 1)
            markConsumed(match.range)
        }
        STRING_REGEX.findAll(code).forEach { match ->
            if (match.range.first !in code.indices || !consumed[match.range.first]) {
                addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
                markConsumed(match.range)
            }
        }
        NUMBER_REGEX.findAll(code).forEach { match ->
            if (!consumed[match.range.first]) {
                addStyle(SpanStyle(color = numberColor), match.range.first, match.range.last + 1)
            }
        }
        IDENTIFIER_REGEX.findAll(code).forEach { match ->
            if (match.value in KEYWORDS && !consumed[match.range.first]) {
                addStyle(
                    SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold),
                    match.range.first,
                    match.range.last + 1
                )
            }
        }
    }
}

/** Applies [SyntaxHighlighter] live inside an editable TextField — text length/offsets
 * never change (only colour spans are added), so the offset mapping is the identity. */
class SyntaxHighlightTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(SyntaxHighlighter.highlight(text.text), OffsetMapping.Identity)
}
