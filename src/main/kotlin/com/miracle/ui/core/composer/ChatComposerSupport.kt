package com.miracle.ui.core.composer

import com.miracle.ui.core.AssociatedContextItem
import com.miracle.utils.addLineNumbers
import com.miracle.utils.toRelativePath
import com.miracle.utils.truncateToCharBudget
import kotlin.io.path.Path
import kotlin.io.path.extension

data class PlaceholderSnapshot(
    val startOffset: Int,
    val endOffset: Int,
    val label: String,
    val content: String,
)

data class TokenRange(
    val startOffset: Int,
    val endOffset: Int,
)

object ChatComposerSupport {

    fun expandText(text: String, placeholders: List<PlaceholderSnapshot>): String {
        if (placeholders.isEmpty()) return text

        val validPlaceholders = placeholders
            .sortedBy { it.startOffset }
            .filter { it.startOffset >= 0 && it.endOffset <= text.length && it.startOffset < it.endOffset }
        if (validPlaceholders.isEmpty()) return text

        val result = StringBuilder()
        var cursor = 0
        for (placeholder in validPlaceholders) {
            if (placeholder.startOffset < cursor) continue
            if (cursor < placeholder.startOffset) {
                result.append(text, cursor, placeholder.startOffset)
            }
            val span = text.substring(placeholder.startOffset, placeholder.endOffset)
            if (span == placeholder.label) {
                result.append(placeholder.content)
            } else {
                result.append(span)
            }
            cursor = placeholder.endOffset
        }
        if (cursor < text.length) {
            result.append(text.substring(cursor))
        }
        return result.toString()
    }

    fun findSearchTextAfterAt(text: String, caretOffset: Int): String? {
        if (caretOffset < 0 || caretOffset > text.length) return null
        val atPos = text.lastIndexOf('@', startIndex = (caretOffset - 1).coerceAtLeast(0))
        if (atPos == -1 || atPos >= caretOffset) return null

        val searchText = text.substring(atPos + 1, caretOffset)
        return if (searchText.any { it.isWhitespace() }) null else searchText
    }

    fun findAtTokenRange(text: CharSequence, caretOffset: Int): TokenRange? {
        if (caretOffset < 0 || caretOffset > text.length) return null
        var cursor = caretOffset - 1
        while (cursor >= 0) {
            when (val current = text[cursor]) {
                '@' -> return TokenRange(cursor, caretOffset)
                else -> if (current.isWhitespace()) return null
            }
            cursor--
        }
        return null
    }

    fun findSlashTokenRange(text: CharSequence, caretOffset: Int): TokenRange? {
        if (caretOffset < 0 || caretOffset > text.length) return null
        val slashStart = findSlashStart(text, caretOffset) ?: return null

        var tokenEnd = slashStart + 1
        while (tokenEnd < text.length && isSlashTokenChar(text[tokenEnd])) {
            tokenEnd++
        }
        if (caretOffset < slashStart || caretOffset > tokenEnd) return null
        return TokenRange(slashStart, tokenEnd)
    }

    fun findSlashSearchText(text: String, caretOffset: Int): String? {
        val range = findSlashTokenRange(text, caretOffset) ?: return null
        return text.substring(range.startOffset, caretOffset.coerceAtMost(range.endOffset))
    }

    fun resolveSlashLookupScope(text: String, caretOffset: Int): SlashCommandScope {
        val range = findSlashTokenRange(text, caretOffset) ?: return SlashCommandScope.SKILLS_ONLY
        val prefix = text.substring(0, range.startOffset)
        val suffix = text.substring(range.endOffset)
        return if (prefix.isBlank() && suffix.isBlank()) {
            SlashCommandScope.ALL
        } else {
            SlashCommandScope.SKILLS_ONLY
        }
    }

    fun buildSlashInvocationText(command: SlashCommand, appendTrailingSpace: Boolean = false): String {
        if (command.argumentTemplates.isEmpty()) {
            return if (appendTrailingSpace) {
                command.command + " "
            } else {
                command.command
            }
        }
        return buildString {
            append(command.command)
            append(' ')
            append(command.argumentTemplates.joinToString(" "))
        }
    }

    fun replaceSlashToken(text: String, caretOffset: Int, replacement: String): Pair<String, Int> {
        val slashRange = findSlashTokenRange(text, caretOffset)
        return if (slashRange == null) {
            val safeOffset = caretOffset.coerceIn(0, text.length)
            val updated = text.substring(0, safeOffset) + replacement + text.substring(safeOffset)
            updated to safeOffset
        } else {
            val updated = text.substring(0, slashRange.startOffset) + replacement + text.substring(slashRange.endOffset)
            updated to slashRange.startOffset
        }
    }

    fun firstPlaceholderSelectionRange(startOffset: Int, invocationText: String): TokenRange? {
        val placeholderRegex = Regex("""<([^>\n\r]+)>""")
        val firstPlaceholder = placeholderRegex.find(invocationText) ?: return null
        val rangeStart = startOffset + firstPlaceholder.range.first + 1
        val rangeEnd = startOffset + firstPlaceholder.range.last
        return if (rangeStart >= rangeEnd) null else TokenRange(rangeStart, rangeEnd)
    }

    fun formatPathReference(path: String, basePath: String): String {
        return "`" + toRelativePath(path, basePath) + "`"
    }

    fun formatCodeReference(
        filePath: String,
        fullLineText: String,
        startLine: Int,
        endLine: Int,
    ): String {
        val fileExt = Path(filePath).extension
        var content = addLineNumbers(fullLineText, startLine)
        content = truncateToCharBudget(content, 10_000)
        return buildString {
            append("\n")
            append("```")
            append(fileExt)
            append(":")
            append(filePath)
            append("\n")
            append(content)
            append("\n```\n")
        }
    }

    fun buildAssociatedCodeContext(
        selections: List<AssociatedContextItem.AssociatedCodeSelection>,
    ): String {
        if (selections.isEmpty()) return ""
        val blocks = selections.joinToString("\n\n") { selection ->
            formatCodeReference(
                filePath = selection.filePath,
                fullLineText = selection.fullLineText,
                startLine = selection.startLine,
                endLine = selection.endLine,
            ).trim()
        }
        return "引用的代码上下文：\n\n$blocks"
    }

    fun appendAssociatedCodeContext(
        text: String,
        selections: List<AssociatedContextItem.AssociatedCodeSelection>,
    ): String {
        val context = buildAssociatedCodeContext(selections)
        if (context.isBlank()) return text
        val trimmedText = text.trim()
        return if (trimmedText.isBlank()) {
            context
        } else {
            "$trimmedText\n\n$context"
        }
    }

    fun findMatchedSlashCommandRanges(
        text: String,
        knownCommands: Set<String>,
    ): List<TokenRange> {
        if (text.isEmpty() || knownCommands.isEmpty() || !text.contains('/')) return emptyList()

        val ranges = mutableListOf<TokenRange>()
        var cursor = 0
        while (cursor < text.length) {
            if (text[cursor] == '/' && (cursor == 0 || text[cursor - 1] != '/')) {
                var end = cursor + 1
                while (end < text.length && isSlashTokenChar(text[end])) {
                    end++
                }
                if (end > cursor + 1) {
                    val token = text.substring(cursor, end).lowercase()
                    if (token in knownCommands) {
                        ranges += TokenRange(cursor, end)
                    }
                }
                cursor = end
                continue
            }
            cursor++
        }
        return ranges
    }

    fun findSlashArgumentPlaceholderRanges(
        text: String,
        slashRanges: List<TokenRange>,
    ): List<TokenRange> {
        val angleRegex = Regex("""<([^>\n\r]+)>""")
        val ranges = mutableListOf<TokenRange>()
        slashRanges.forEach { range ->
            val lineEnd = findLineEndOffset(text, range.endOffset)
            if (range.endOffset >= lineEnd) return@forEach

            val lineSegment = text.substring(range.endOffset, lineEnd)
            angleRegex.findAll(lineSegment).forEach { match ->
                val start = range.endOffset + match.range.first + 1
                val end = range.endOffset + match.range.last
                if (start < end) {
                    ranges += TokenRange(start, end)
                }
            }
        }
        return ranges.distinct()
    }

    fun findMatchedSlashCommandStart(
        text: String,
        endOffset: Int,
        knownCommands: List<String>,
    ): Int? {
        if (endOffset <= 0 || knownCommands.isEmpty()) return null
        val lowerText = text.lowercase()
        knownCommands.forEach { command ->
            if (endOffset < command.length) return@forEach
            val start = endOffset - command.length
            if (lowerText.substring(start, endOffset) != command) return@forEach
            if (text[start] != '/') return@forEach
            if (start > 0 && text[start - 1] == '/') return@forEach
            return start
        }
        return null
    }

    private fun findSlashStart(text: CharSequence, caretOffset: Int): Int? {
        if (text.isEmpty() || caretOffset <= 0) return null
        var cursor = (caretOffset - 1).coerceAtLeast(0)
        while (cursor >= 0) {
            val current = text[cursor]
            if (current == '/') {
                return cursor
            }
            if (current.isWhitespace()) {
                return null
            }
            cursor--
        }
        return null
    }

    private fun isSlashTokenChar(ch: Char): Boolean {
        return ch.isLetterOrDigit() || ch == '-' || ch == '_' || ch == ':' || ch == '.'
    }

    private fun findLineEndOffset(text: String, startOffset: Int): Int {
        var cursor = startOffset.coerceAtLeast(0)
        while (cursor < text.length) {
            if (text[cursor] == '\n' || text[cursor] == '\r') {
                break
            }
            cursor++
        }
        return cursor
    }
}
