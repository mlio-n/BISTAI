package com.muzaffer.bistai.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muzaffer.bistai.presentation.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiQuickActionBottomSheet(
    title: String,
    subtitle: String,
    isLoading: Boolean,
    markdownResult: String?,
    errorMessage: String?,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when {
                    isLoading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "BISTAI analitik raporu hazırlıyor...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Canlı veriler ve bağlam analiz ediliyor",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    errorMessage != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Analiz Tamamlanamadı",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Tekrar Dene", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    markdownResult != null -> {
                        MarkdownContent(
                            markdown = markdownResult,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class BulletList(val items: List<String>) : MarkdownBlock
    data class NumberedList(val items: List<Pair<String, String>>) : MarkdownBlock
    data class Blockquote(val text: String) : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data object Divider : MarkdownBlock
}

@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    val primaryTextColor = MaterialTheme.colorScheme.onSurface
    val boldColor = MaterialTheme.colorScheme.onBackground
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val textStyle = when (block.level) {
                        1 -> MaterialTheme.typography.titleMedium
                        2 -> MaterialTheme.typography.titleSmall
                        else -> MaterialTheme.typography.bodyMedium
                    }
                    Text(
                        text = buildInlineMarkdown(block.text, boldColor, boldColor, boldColor, codeBackground),
                        style = textStyle,
                        fontWeight = FontWeight.Bold,
                        color = boldColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildInlineMarkdown(block.text, primaryTextColor, boldColor, boldColor, codeBackground),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 21.sp,
                        color = primaryTextColor
                    )
                }

                is MarkdownBlock.BulletList -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        block.items.forEach { item ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    color = boldColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = buildInlineMarkdown(item, primaryTextColor, boldColor, boldColor, codeBackground),
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 20.sp,
                                    color = primaryTextColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.NumberedList -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        block.items.forEach { (num, item) ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "$num.",
                                    color = boldColor,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = buildInlineMarkdown(item, primaryTextColor, boldColor, boldColor, codeBackground),
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 20.sp,
                                    color = primaryTextColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                is MarkdownBlock.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(WarningAmber.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .background(WarningAmber)
                        )
                        Text(
                            text = buildInlineMarkdown(block.text, primaryTextColor, boldColor, boldColor, codeBackground),
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 19.sp,
                            color = primaryTextColor
                        )
                    }
                }

                is MarkdownBlock.Table -> {
                    MarkdownTable(headers = block.headers, rows = block.rows)
                }

                MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownTable(
    headers: List<String>,
    rows: List<List<String>>
) {
    val columnCount = headers.size.coerceAtLeast(1)
    val columnWidth = if (columnCount <= 2) 150.dp else 124.dp
    val headerTextColor = MaterialTheme.colorScheme.onSurface
    val cellTextColor = MaterialTheme.colorScheme.onSurface
    val boldColor = MaterialTheme.colorScheme.onBackground
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(vertical = 10.dp, horizontal = 10.dp)
        ) {
            headers.forEach { header ->
                Text(
                    text = buildInlineMarkdown(header, headerTextColor, boldColor, boldColor, codeBackground),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = headerTextColor,
                    modifier = Modifier
                        .width(columnWidth)
                        .padding(end = 8.dp)
                )
            }
        }

        rows.forEachIndexed { index, row ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier
                    .background(
                        if (index % 2 == 0) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface
                    )
                    .padding(vertical = 9.dp, horizontal = 10.dp)
            ) {
                for (colIdx in 0 until columnCount) {
                    val cellText = row.getOrElse(colIdx) { "" }
                    Text(
                        text = buildInlineMarkdown(cellText, cellTextColor, boldColor, boldColor, codeBackground),
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp,
                        color = cellTextColor,
                        modifier = Modifier
                            .width(columnWidth)
                            .padding(end = 8.dp)
                    )
                }
            }
        }
    }
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val lines = markdown.replace("\r\n", "\n").lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var i = 0

    while (i < lines.size) {
        val rawLine = lines[i]
        val line = rawLine.trim()

        if (line.isEmpty()) {
            i++
            continue
        }

        if (isHorizontalRule(line)) {
            blocks.add(MarkdownBlock.Divider)
            i++
            continue
        }

        if (line.startsWith("#")) {
            val level = line.takeWhile { it == '#' }.length.coerceIn(1, 4)
            val headingText = line.drop(level).trim()
            if (headingText.isNotEmpty()) {
                blocks.add(MarkdownBlock.Heading(level, headingText))
            }
            i++
            continue
        }

        if (line.contains("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1])) {
            val headers = splitTableRow(line)
            i += 2
            val rows = mutableListOf<List<String>>()
            while (i < lines.size) {
                val rowLine = lines[i].trim()
                if (rowLine.isEmpty() || !rowLine.contains("|")) break
                rows.add(splitTableRow(rowLine))
                i++
            }
            if (headers.isNotEmpty()) {
                blocks.add(MarkdownBlock.Table(headers, rows))
            }
            continue
        }

        if (line.startsWith(">")) {
            val quoteLines = mutableListOf<String>()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                quoteLines.add(lines[i].trim().removePrefix(">").trim())
                i++
            }
            blocks.add(MarkdownBlock.Blockquote(quoteLines.joinToString("\n")))
            continue
        }

        if (isBulletLine(line)) {
            val items = mutableListOf<String>()
            while (i < lines.size && isBulletLine(lines[i].trim())) {
                items.add(lines[i].trim().drop(2).trim())
                i++
            }
            blocks.add(MarkdownBlock.BulletList(items))
            continue
        }

        val numberedMatch = NUMBERED_LIST_REGEX.matchEntire(line)
        if (numberedMatch != null) {
            val items = mutableListOf<Pair<String, String>>()
            while (i < lines.size) {
                val m = NUMBERED_LIST_REGEX.matchEntire(lines[i].trim()) ?: break
                items.add(m.groupValues[1] to m.groupValues[2].trim())
                i++
            }
            blocks.add(MarkdownBlock.NumberedList(items))
            continue
        }

        val paragraphLines = mutableListOf<String>()
        while (i < lines.size) {
            val candidate = lines[i].trim()
            if (candidate.isEmpty() ||
                candidate.startsWith("#") ||
                candidate.startsWith(">") ||
                isHorizontalRule(candidate) ||
                isBulletLine(candidate) ||
                NUMBERED_LIST_REGEX.matches(candidate) ||
                (candidate.contains("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1]))
            ) {
                break
            }
            paragraphLines.add(candidate)
            i++
        }
        if (paragraphLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString(" ")))
        }
    }

    return blocks
}

private val NUMBERED_LIST_REGEX = Regex("""^(\d+)[.)]\s+(.+)$""")

private fun isHorizontalRule(line: String): Boolean {
    val compact = line.replace(" ", "")
    return compact.length >= 3 && (compact.all { it == '-' } || compact.all { it == '*' } || compact.all { it == '_' })
}

private fun isBulletLine(line: String): Boolean =
    line.startsWith("- ") || line.startsWith("* ") || line.startsWith("• ")

private fun isTableSeparator(line: String): Boolean {
    val trimmed = line.trim()
    if (!trimmed.contains("-")) return false
    val stripped = trimmed.removePrefix("|").removeSuffix("|").split("|")
    return stripped.isNotEmpty() && stripped.all { cell ->
        val c = cell.trim()
        c.isNotEmpty() && c.all { it == '-' || it == ':' }
    }
}

private fun splitTableRow(line: String): List<String> =
    line.trim()
        .removePrefix("|")
        .removeSuffix("|")
        .split("|")
        .map { it.trim() }

private fun buildInlineMarkdown(
    text: String,
    baseColor: Color,
    boldColor: Color,
    codeColor: Color,
    codeBackground: Color
): AnnotatedString {
    return buildAnnotatedString {
        var idx = 0
        while (idx < text.length) {
            when {
                text.startsWith("**", idx) -> {
                    val end = text.indexOf("**", idx + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                            append(text.substring(idx + 2, end))
                        }
                        idx = end + 2
                    } else {
                        append("**")
                        idx += 2
                    }
                }
                text.startsWith("__", idx) -> {
                    val end = text.indexOf("__", idx + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                            append(text.substring(idx + 2, end))
                        }
                        idx = end + 2
                    } else {
                        append("__")
                        idx += 2
                    }
                }
                text[idx] == '`' -> {
                    val end = text.indexOf('`', idx + 1)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                color = codeColor,
                                background = codeBackground
                            )
                        ) {
                            append(text.substring(idx + 1, end))
                        }
                        idx = end + 1
                    } else {
                        append('`')
                        idx++
                    }
                }
                text[idx] == '*' && (idx + 1 >= text.length || text[idx + 1] != '*') -> {
                    val end = text.indexOf('*', idx + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor)) {
                            append(text.substring(idx + 1, end))
                        }
                        idx = end + 1
                    } else {
                        append('*')
                        idx++
                    }
                }
                else -> {
                    withStyle(SpanStyle(color = baseColor)) {
                        append(text[idx])
                    }
                    idx++
                }
            }
        }
    }
}
