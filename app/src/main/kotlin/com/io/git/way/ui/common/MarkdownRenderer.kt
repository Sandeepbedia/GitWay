/*
 * Git Way
 * Copyright (C) 2026 Sandeep Bedia
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.io.git.way.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Resolves a possibly-relative markdown link/image target against the repo it came
 * from — e.g. a README under `docs/` that links to `../assets/banner.png` needs to
 * become an absolute raw.githubusercontent.com URL before Coil can load it, and a plain
 * `CONTRIBUTING.md` link needs to become a real github.com blob URL to be openable.
 */
class MarkdownLinkResolver(
    private val owner: String,
    private val repo: String,
    private val branch: String,
    /** Directory the file being previewed lives in, e.g. "docs" for "docs/README.md",
     * or "" for a root-level README.md. */
    private val currentFileDir: String
) {
    /** Absolute URL to fetch raw bytes from — what an `<img>`/`![]()` needs. */
    fun resolveRaw(url: String): String {
        if (isAbsolute(url)) return url
        return "https://raw.githubusercontent.com/$owner/$repo/$branch/${resolvePath(url)}"
    }

    /** Absolute URL to a browsable GitHub page — what a `[text](link)` should open to. */
    fun resolveBlob(url: String): String {
        if (isAbsolute(url)) return url
        if (url.startsWith("#")) return "https://github.com/$owner/$repo/blob/$branch/$currentFileDir$url"
        return "https://github.com/$owner/$repo/blob/$branch/${resolvePath(url)}"
    }

    private fun isAbsolute(url: String) =
        url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:") || url.startsWith("mailto:")

    private fun resolvePath(url: String): String {
        val clean = url.substringBefore('#').substringBefore('?')
        if (clean.startsWith("/")) return clean.trim('/')
        val segments = if (currentFileDir.isEmpty()) mutableListOf() else currentFileDir.split('/').toMutableList()
        for (seg in clean.split('/')) {
            when (seg) {
                ".", "" -> Unit
                ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.size - 1)
                else -> segments.add(seg)
            }
        }
        return segments.joinToString("/")
    }
}

private sealed class MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock()
    data class Paragraph(val text: String) : MdBlock()
    data class CodeBlock(val code: String) : MdBlock()
    data class ListItem(
        val text: String,
        val ordered: Boolean,
        val number: Int,
        val indent: Int = 0,
        /** null = plain bullet, otherwise a GFM task-list checkbox ("- [ ]" / "- [x]"). */
        val checked: Boolean? = null
    ) : MdBlock()
    data class Quote(val text: String) : MdBlock()
    data class ImageBlock(val alt: String, val url: String) : MdBlock()
    data class Table(val header: List<String>, val alignments: List<TableAlign>, val rows: List<List<String>>) : MdBlock()
    data object Rule : MdBlock()
}

private enum class TableAlign { LEFT, CENTER, RIGHT }

private object MarkdownParser {
    private val imageOnlyLine = Regex("""^!\[([^]]*)]\(([^)]+)\)\s*$""")
    private val headingLine = Regex("""^#{1,6}\s+.*""")
    private val ruleLine = Regex("""^(-{3,}|\*{3,}|_{3,})$""")
    private val orderedItem = Regex("""^\d+\.\s+.*""")
    private val taskItem = Regex("""^[-*+]\s+\[([ xX])]\s+(.*)$""")
    /** A GFM table separator row: `|---|:---:|---:|` (with optional leading/trailing `|`). */
    private val tableSeparator = Regex("""^\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?$""")
    /** Strips block-level HTML tags GFM READMEs commonly use for layout
     * (`<div align="center">`, `<p>`, `<br>`, `<details>/<summary>`, comments) — Git Way
     * doesn't render real HTML, so these are dropped rather than shown as raw markup;
     * their *text content* (e.g. inside `<summary>click me</summary>`) is kept. */
    private val htmlTag = Regex("""</?[a-zA-Z][^>]*>""")
    private val htmlComment = Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL)

    private fun stripHtml(line: String): String = line.replace(htmlComment, "").replace(htmlTag, "").trim()

    private fun splitTableRow(line: String): List<String> {
        var trimmed = line.trim().removePrefix("|").removeSuffix("|")
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < trimmed.length) {
            val c = trimmed[i]
            if (c == '\\' && i + 1 < trimmed.length && trimmed[i + 1] == '|') {
                current.append('|'); i += 2; continue
            }
            if (c == '|') { cells += current.toString().trim(); current.clear() } else current.append(c)
            i++
        }
        cells += current.toString().trim()
        return cells
    }

    fun parse(markdown: String): List<MdBlock> {
        val lines = markdown.replace("\r\n", "\n").split("\n")
        val blocks = mutableListOf<MdBlock>()
        var i = 0
        var orderedCounter = 0

        fun bulletIndent(l: String): Int = (l.takeWhile { it == ' ' }.length) / 2
        fun isListLine(l: String) = l.trimStart().let { it.startsWith("- ") || it.startsWith("* ") || it.startsWith("+ ") }

        while (i < lines.size) {
            val rawLine = lines[i]
            val line = if (rawLine.contains('<')) stripHtml(rawLine) else rawLine
            val trimmed = line.trim()
            when {
                line.isBlank() -> { orderedCounter = 0; i++ }

                line.startsWith("```") -> {
                    i++
                    val codeLines = mutableListOf<String>()
                    while (i < lines.size && !lines[i].startsWith("```")) {
                        codeLines += lines[i]; i++
                    }
                    if (i < lines.size) i++ // consume closing fence
                    blocks += MdBlock.CodeBlock(codeLines.joinToString("\n"))
                    orderedCounter = 0
                }

                headingLine.matches(trimmed) -> {
                    val level = trimmed.takeWhile { it == '#' }.length
                    blocks += MdBlock.Heading(level, trimmed.drop(level).trim())
                    i++; orderedCounter = 0
                }

                trimmed.startsWith(">") -> {
                    blocks += MdBlock.Quote(trimmed.removePrefix(">").trim())
                    i++; orderedCounter = 0
                }

                ruleLine.matches(trimmed.replace(" ", "")) -> {
                    blocks += MdBlock.Rule
                    i++; orderedCounter = 0
                }

                imageOnlyLine.matches(trimmed) -> {
                    val m = imageOnlyLine.find(trimmed)!!
                    blocks += MdBlock.ImageBlock(m.groupValues[1], m.groupValues[2])
                    i++; orderedCounter = 0
                }

                // GFM table: a "| a | b |" row immediately followed by a "|---|---|" rule.
                trimmed.startsWith("|") && i + 1 < lines.size && tableSeparator.matches(lines[i + 1].trim()) -> {
                    val header = splitTableRow(trimmed)
                    val alignCells = splitTableRow(lines[i + 1].trim())
                    val alignments = header.indices.map { col ->
                        val spec = alignCells.getOrNull(col)?.trim().orEmpty()
                        when {
                            spec.startsWith(":") && spec.endsWith(":") -> TableAlign.CENTER
                            spec.endsWith(":") -> TableAlign.RIGHT
                            else -> TableAlign.LEFT
                        }
                    }
                    i += 2
                    val rows = mutableListOf<List<String>>()
                    while (i < lines.size && lines[i].trim().startsWith("|")) {
                        rows += splitTableRow(lines[i].trim())
                        i++
                    }
                    blocks += MdBlock.Table(header, alignments, rows)
                    orderedCounter = 0
                }

                taskItem.matches(trimmed) -> {
                    val m = taskItem.find(trimmed)!!
                    blocks += MdBlock.ListItem(
                        text = m.groupValues[2].trim(),
                        ordered = false,
                        number = 0,
                        indent = bulletIndent(line),
                        checked = m.groupValues[1].equals("x", ignoreCase = true)
                    )
                    i++
                }

                isListLine(line) -> {
                    blocks += MdBlock.ListItem(
                        text = line.trimStart().drop(2).trim(),
                        ordered = false,
                        number = 0,
                        indent = bulletIndent(line)
                    )
                    i++
                }

                orderedItem.matches(trimmed) -> {
                    orderedCounter++
                    blocks += MdBlock.ListItem(
                        text = trimmed.substringAfter(". ").trim(),
                        ordered = true,
                        number = orderedCounter,
                        indent = bulletIndent(line)
                    )
                    i++
                }

                else -> {
                    val paraLines = mutableListOf(line)
                    i++
                    while (i < lines.size && lines[i].isNotBlank() &&
                        !lines[i].startsWith("```") &&
                        !headingLine.matches(lines[i].trim()) &&
                        !lines[i].trim().startsWith(">") &&
                        !isListLine(lines[i]) &&
                        !taskItem.matches(lines[i].trim())
                    ) {
                        paraLines += (if (lines[i].contains('<')) stripHtml(lines[i]) else lines[i]); i++
                    }
                    val paragraphText = paraLines.joinToString(" ") { it.trim() }.trim()
                    if (paragraphText.isNotEmpty()) blocks += MdBlock.Paragraph(paragraphText)
                    orderedCounter = 0
                }
            }
        }
        return blocks
    }
}

/** Parses `**bold**`, `*italic*`, `` `code` `` and `[text](url)` inline markdown into a
 * clickable [androidx.compose.ui.text.AnnotatedString]. */
private fun inlineAnnotated(text: String, resolver: MarkdownLinkResolver, linkColor: Color) = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end == -1) { append(text[i]); i++ } else {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                    i = end + 2
                }
            }
            text.startsWith("~~", i) -> {
                val end = text.indexOf("~~", i + 2)
                if (end == -1) { append(text[i]); i++ } else {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(text.substring(i + 2, end)) }
                    i = end + 2
                }
            }
            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end == -1) { append(text[i]); i++ } else {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color.Gray.copy(alpha = 0.22f)
                        )
                    ) { append(text.substring(i + 1, end)) }
                    i = end + 1
                }
            }
            text[i] == '*' && !text.startsWith("**", i) -> {
                val end = text.indexOf('*', i + 1)
                if (end == -1) { append(text[i]); i++ } else {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                }
            }
            // Inline image/badge — `![alt](img)`, optionally wrapped in a link:
            // `[![alt](img)](href)`. Compose's plain AnnotatedString can't embed a real
            // <img> inline, so this renders as clickable alt-text instead of leaking the
            // raw markdown syntax onto the screen (badges are almost always short labels
            // like "build passing", so the text alone stays meaningful).
            text.startsWith("[![", i) -> {
                val altClose = text.indexOf(']', i + 3)
                val imgUrlClose = if (altClose != -1 && text.getOrNull(altClose + 1) == '(') text.indexOf(')', altClose) else -1
                val linkClose = if (imgUrlClose != -1 && text.getOrNull(imgUrlClose + 1) == ']' && text.getOrNull(imgUrlClose + 2) == '(')
                    text.indexOf(')', imgUrlClose + 2) else -1
                if (altClose != -1 && imgUrlClose != -1 && linkClose != -1) {
                    val alt = text.substring(i + 3, altClose).ifBlank { "badge" }
                    val href = text.substring(imgUrlClose + 3, linkClose)
                    withLink(LinkAnnotation.Url(resolver.resolveBlob(href))) {
                        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) { append(alt) }
                    }
                    i = linkClose + 1
                } else { append(text[i]); i++ }
            }
            text.startsWith("![", i) -> {
                val altClose = text.indexOf(']', i + 2)
                val urlClose = if (altClose != -1 && text.getOrNull(altClose + 1) == '(') text.indexOf(')', altClose) else -1
                if (altClose != -1 && urlClose != -1) {
                    val alt = text.substring(i + 2, altClose).ifBlank { "image" }
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = linkColor)) { append(alt) }
                    i = urlClose + 1
                } else { append(text[i]); i++ }
            }
            text[i] == '[' -> {
                val closeBracket = text.indexOf(']', i)
                val hasUrl = closeBracket != -1 && closeBracket + 1 < text.length && text[closeBracket + 1] == '('
                val closeParen = if (hasUrl) text.indexOf(')', closeBracket) else -1
                if (hasUrl && closeParen != -1) {
                    val label = text.substring(i + 1, closeBracket)
                    val url = text.substring(closeBracket + 2, closeParen)
                    withLink(LinkAnnotation.Url(resolver.resolveBlob(url))) {
                        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                            append(label)
                        }
                    }
                    i = closeParen + 1
                } else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}

/** Renders GitHub-flavoured README markdown — headings, bold/italic/inline-code, links
 * that open the real GitHub page, images loaded from raw.githubusercontent.com, lists,
 * block quotes, fenced code blocks and horizontal rules. Built without a third-party
 * markdown library: just enough of GFM to make a typical README readable, kept as plain
 * Compose so it reflows correctly at any width — phone, foldable, or a DeX/Chromebook
 * "desktop mode" window. */
@Composable
fun MarkdownView(markdown: String, resolver: MarkdownLinkResolver, modifier: Modifier = Modifier) {
    val blocks = remember(markdown) { MarkdownParser.parse(markdown) }
    val linkColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text = inlineAnnotated(block.text, resolver, linkColor),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineSmall
                        2 -> MaterialTheme.typography.titleLarge
                        3 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                )

                is MdBlock.Paragraph -> if (block.text.isNotBlank()) {
                    Text(text = inlineAnnotated(block.text, resolver, linkColor), style = MaterialTheme.typography.bodyMedium)
                }

                is MdBlock.CodeBlock -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Text(
                        SyntaxHighlighter.highlight(block.code),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                is MdBlock.ListItem -> Row(modifier = Modifier.padding(start = (6 + block.indent * 18).dp)) {
                    when {
                        block.checked != null -> Icon(
                            if (block.checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (block.checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 6.dp).size(18.dp)
                        )
                        else -> Text(
                            text = if (block.ordered) "${block.number}." else if (block.indent > 0) "◦" else "•",
                            modifier = Modifier.padding(end = 6.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = inlineAnnotated(block.text, resolver, linkColor),
                        style = MaterialTheme.typography.bodyMedium,
                        textDecoration = if (block.checked == true) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (block.checked == true) MaterialTheme.colorScheme.onSurfaceVariant else LocalContentColor.current
                    )
                }

                is MdBlock.Table -> MarkdownTable(block, resolver, linkColor)

                is MdBlock.Quote -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = inlineAnnotated(block.text, resolver, linkColor),
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                is MdBlock.ImageBlock -> AsyncImage(
                    model = resolver.resolveRaw(block.url),
                    contentDescription = block.alt,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.FillWidth
                )

                MdBlock.Rule -> HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

/** GFM table, rendered close to how GitHub shows it: bordered cells, a bold/tinted
 * header row, alternating row shading, and horizontal scroll for tables wider than the
 * screen (very common for READMEs with many columns) instead of wrapping/squeezing text. */
@Composable
private fun MarkdownTable(table: MdBlock.Table, resolver: MarkdownLinkResolver, linkColor: Color) {
    val borderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    val headerBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val stripeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    val columnWidth = 160.dp

    Column(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.background(headerBg)) {
            table.header.forEachIndexed { col, cell ->
                Text(
                    text = inlineAnnotated(cell, resolver, linkColor),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = when (table.alignments.getOrNull(col)) {
                        TableAlign.CENTER -> TextAlign.Center
                        TableAlign.RIGHT -> TextAlign.End
                        else -> TextAlign.Start
                    },
                    modifier = Modifier
                        .width(columnWidth)
                        .border(0.5.dp, borderColor)
                        .padding(8.dp)
                )
            }
        }
        table.rows.forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.background(if (rowIndex % 2 == 1) stripeBg else Color.Transparent)) {
                table.header.indices.forEach { col ->
                    Text(
                        text = inlineAnnotated(row.getOrElse(col) { "" }, resolver, linkColor),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = when (table.alignments.getOrNull(col)) {
                            TableAlign.CENTER -> TextAlign.Center
                            TableAlign.RIGHT -> TextAlign.End
                            else -> TextAlign.Start
                        },
                        modifier = Modifier
                            .width(columnWidth)
                            .border(0.5.dp, borderColor)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
