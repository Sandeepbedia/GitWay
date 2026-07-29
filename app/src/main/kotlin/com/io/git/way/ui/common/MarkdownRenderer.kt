package com.io.git.way.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
    data class ListItem(val text: String, val ordered: Boolean, val number: Int) : MdBlock()
    data class Quote(val text: String) : MdBlock()
    data class ImageBlock(val alt: String, val url: String) : MdBlock()
    data object Rule : MdBlock()
}

private object MarkdownParser {
    private val imageOnlyLine = Regex("""^!\[([^]]*)]\(([^)]+)\)\s*$""")
    private val headingLine = Regex("""^#{1,6}\s+.*""")
    private val ruleLine = Regex("""^(-{3,}|\*{3,}|_{3,})$""")
    private val orderedItem = Regex("""^\d+\.\s+.*""")

    fun parse(markdown: String): List<MdBlock> {
        val lines = markdown.replace("\r\n", "\n").split("\n")
        val blocks = mutableListOf<MdBlock>()
        var i = 0
        var orderedCounter = 0

        fun isListLine(l: String) = l.trimStart().let { it.startsWith("- ") || it.startsWith("* ") || it.startsWith("+ ") }

        while (i < lines.size) {
            val line = lines[i]
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

                isListLine(line) -> {
                    blocks += MdBlock.ListItem(line.trimStart().drop(2).trim(), ordered = false, number = 0)
                    i++
                }

                orderedItem.matches(trimmed) -> {
                    orderedCounter++
                    blocks += MdBlock.ListItem(trimmed.substringAfter(". ").trim(), ordered = true, number = orderedCounter)
                    i++
                }

                else -> {
                    val paraLines = mutableListOf(line)
                    i++
                    while (i < lines.size && lines[i].isNotBlank() &&
                        !lines[i].startsWith("```") &&
                        !headingLine.matches(lines[i].trim()) &&
                        !lines[i].trim().startsWith(">") &&
                        !isListLine(lines[i])
                    ) {
                        paraLines += lines[i]; i++
                    }
                    blocks += MdBlock.Paragraph(paraLines.joinToString(" ") { it.trim() }.trim())
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

                is MdBlock.ListItem -> Row(modifier = Modifier.padding(start = 6.dp)) {
                    Text(
                        text = if (block.ordered) "${block.number}." else "•",
                        modifier = Modifier.padding(end = 6.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(text = inlineAnnotated(block.text, resolver, linkColor), style = MaterialTheme.typography.bodyMedium)
                }

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
