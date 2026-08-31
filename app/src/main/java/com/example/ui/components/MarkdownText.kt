package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.PurpleNeural
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate950

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock()
    object Divider : MarkdownBlock()
}

@Composable
fun MarkdownContent(
    content: String,
    modifier: Modifier = Modifier
) {
    val blocks = parseMarkdownBlocks(content)

    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                    val (style, color) = when (block.level) {
                        1 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp) to CyanNeon
                        2 -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp) to CyanAccent
                        else -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp) to Color.White
                    }
                    Text(
                        text = block.text,
                        style = style,
                        color = color,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }

                is MarkdownBlock.CodeBlock -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    CodeBlockCard(
                        language = block.language,
                        code = block.code
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = CyanNeon,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FormattedInlineText(
                            text = block.text,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${block.number}.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = PurpleNeural,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FormattedInlineText(
                            text = block.text,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.Divider -> {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Slate700)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                is MarkdownBlock.Paragraph -> {
                    if (block.text.isNotBlank()) {
                        FormattedInlineText(
                            text = block.text,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(
    language: String,
    code: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Slate700, RoundedCornerShape(8.dp)),
        color = Slate950
    ) {
        Column {
            // Code header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate800)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language.isNotBlank()) language.uppercase() else "CODE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Slate400,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Code Snippet", code)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Slate400,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Code content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(10.dp)
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF80D4FF),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

@Composable
fun FormattedInlineText(
    text: String,
    modifier: Modifier = Modifier
) {
    val annotated = buildAnnotatedString {
        val parts = splitInlineStyles(text)
        for (part in parts) {
            when (part.type) {
                InlineType.BOLD -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                        append(part.content)
                    }
                }
                InlineType.ITALIC -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Slate400)) {
                        append(part.content)
                    }
                }
                InlineType.CODE -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = CyanNeon,
                            background = Slate800,
                            fontSize = 12.5.sp
                        )
                    ) {
                        append(" ${part.content} ")
                    }
                }
                InlineType.PLAIN -> {
                    withStyle(SpanStyle(color = Color(0xFFECEFF4))) {
                        append(part.content)
                    }
                }
            }
        }
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        modifier = modifier
    )
}

enum class InlineType { PLAIN, BOLD, ITALIC, CODE }
data class InlinePart(val content: String, val type: InlineType)

fun parseMarkdownBlocks(raw: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = raw.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Fenced code block
        if (line.trimStart().startsWith("```")) {
            val lang = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
            i++
            continue
        }

        // Headers
        if (line.startsWith("#")) {
            val level = line.takeWhile { it == '#' }.length
            val text = line.removePrefix("#".repeat(level)).trim()
            blocks.add(MarkdownBlock.Header(level, text))
            i++
            continue
        }

        // Divider
        if (line.trim() == "---" || line.trim() == "***" || line.trim() == "___") {
            blocks.add(MarkdownBlock.Divider)
            i++
            continue
        }

        // Bullet Item
        if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) {
            val text = line.trimStart().substring(2).trim()
            blocks.add(MarkdownBlock.BulletItem(text))
            i++
            continue
        }

        // Numbered Item
        val numMatch = Regex("^\\s*(\\d+)\\.\\s+(.*)").find(line)
        if (numMatch != null) {
            val num = numMatch.groupValues[1]
            val text = numMatch.groupValues[2]
            blocks.add(MarkdownBlock.NumberedItem(num, text))
            i++
            continue
        }

        // Paragraph
        if (line.isNotBlank()) {
            blocks.add(MarkdownBlock.Paragraph(line))
        }
        i++
    }

    return blocks
}

fun splitInlineStyles(text: String): List<InlinePart> {
    val parts = mutableListOf<InlinePart>()
    val inlineRegex = Regex("(\\*\\*([^*]+)\\*\\*)|(`([^`]+)`)|(\\*([^*]+)\\*)")

    var lastIndex = 0
    val matches = inlineRegex.findAll(text)

    for (match in matches) {
        if (match.range.first > lastIndex) {
            parts.add(InlinePart(text.substring(lastIndex, match.range.first), InlineType.PLAIN))
        }

        val full = match.value
        when {
            full.startsWith("**") && full.endsWith("**") -> {
                parts.add(InlinePart(full.substring(2, full.length - 2), InlineType.BOLD))
            }
            full.startsWith("`") && full.endsWith("`") -> {
                parts.add(InlinePart(full.substring(1, full.length - 1), InlineType.CODE))
            }
            full.startsWith("*") && full.endsWith("*") -> {
                parts.add(InlinePart(full.substring(1, full.length - 1), InlineType.ITALIC))
            }
            else -> {
                parts.add(InlinePart(full, InlineType.PLAIN))
            }
        }
        lastIndex = match.range.last + 1
    }

    if (lastIndex < text.length) {
        parts.add(InlinePart(text.substring(lastIndex), InlineType.PLAIN))
    }

    return if (parts.isEmpty()) listOf(InlinePart(text, InlineType.PLAIN)) else parts
}
