package com.qifu.utils

import com.github.difflib.DiffUtils
import com.github.difflib.patch.Patch

// diff结构中的变更片段（文件中连续的一段变更）
data class Hunk(
    val oldStart: Int,  // 这个变更片段在“原始文件”中从第几行开始
    val oldLines: Int, // 变更前（旧版本）总共涉及的 行数
    val newStart: Int, // 变更后（新版本）开始的 行号（从 1 开始）
    val newLines: Int, // 变更后（新版本）总共涉及的 行数
    val lines: List<String>  //这个 hunk 的实际内容，每行带前缀：- +
)

fun getPatch(
    filePath: String,
    fileContents: String,
    oldStr: String,
    newStr: String,
    contextLines: Int = 3
): List<Hunk> {
    val AMPERSAND_TOKEN = "<<:AMPERSAND_TOKEN:>>"
    val DOLLAR_TOKEN = "<<:DOLLAR_TOKEN:>>"

    fun escape(str: String): String {
        return str.replace("&", AMPERSAND_TOKEN).replace("$", DOLLAR_TOKEN)
    }

    fun unescape(str: String): String {
        return str.replace(AMPERSAND_TOKEN, "&").replace(DOLLAR_TOKEN, "$")
    }

    val escapedContent = escape(fileContents)
    val escapedOldStr = escape(oldStr)
    val escapedNewStr = escape(newStr)

    val replacedContent = escapedContent.replace(escapedOldStr, escapedNewStr)

    val originalLines = escapedContent.lines()
    val revisedLines = replacedContent.lines()

    val patch: Patch<String> = DiffUtils.diff(originalLines, revisedLines)

    val hunks = mutableListOf<Hunk>()

    for (delta in patch.deltas) {
        val origPos = delta.source.position
        val origLines = delta.source.lines
        val revPos = delta.target.position
        val revLines = delta.target.lines

        // 计算 hunk 上下文范围
        val contextStart = maxOf(0, origPos - contextLines)
        val contextEnd = minOf(originalLines.size, origPos + origLines.size + contextLines)

        val originalSlice = originalLines.subList(contextStart, contextEnd)
        val revisedSlice = revisedLines.subList(
            maxOf(0, revPos - contextLines),
            minOf(revisedLines.size, revPos + revLines.size + contextLines)
        )

        // 构建 diff 行（+、-、 空格）
        val lines = mutableListOf<String>()

        val deltaRange = contextEnd - contextStart

        for (i in 0 until deltaRange) {
            val origLineIndex = contextStart + i
            val inOrig = origLineIndex in origPos until (origPos + origLines.size)
            val inRev = origLineIndex - (origPos - revPos) in revPos until (revPos + revLines.size)

            when {
                inOrig && !inRev -> {
                    lines.add("-" + unescape(originalLines[origLineIndex]))
                }
                !inOrig && inRev -> {
                    lines.add("+" + unescape(revisedLines[origLineIndex - (origPos - revPos)]))
                }
                inOrig && inRev -> {
                    val origLine = originalLines[origLineIndex]
                    val revLine = revisedLines[origLineIndex - (origPos - revPos)]
                    if (origLine != revLine) {
                        lines.add("-" + unescape(origLine))
                        lines.add("+" + unescape(revLine))
                    } else {
                        lines.add(" " + unescape(origLine))
                    }
                }
                else -> {
                    // context only
                    lines.add(" " + unescape(originalLines[origLineIndex]))
                }
            }
        }

        val oldStart = contextStart + 1 // 1-based index
        val newStart = revPos - (origPos - contextStart) + 1

        hunks.add(
            Hunk(
                oldStart = oldStart,
                oldLines = originalSlice.size,
                newStart = newStart,
                newLines = revisedSlice.size,
                lines = lines
            )
        )
    }

    return hunks
}

fun parseDiffToContent(diffString: String): String {
    val lines = diffString.lines()
    val result = mutableListOf<String>()
    
    for (line in lines) {
        when {
            line.startsWith("@@") -> continue
            line.startsWith("-") -> continue
            line.startsWith("+") -> result.add(line.substring(1))
            line.startsWith(" ") -> result.add(line.substring(1))
            else -> result.add(line)
        }
    }
    
    return result.joinToString("\n")
}
