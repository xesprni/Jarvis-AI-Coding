package com.qifu.ui.utils

import com.qihoo.finance.lowcode.smartconversation.service.ResponseNodeRenderer
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import java.util.regex.Pattern

object MarkdownUtil {

    /**
     * Splits a given string into a list of strings where each element is either a code block
     * surrounded by triple backticks or a non-code block text.
     *
     * @param inputMarkdown The input markdown formatted string to be split.
     * @return A list of strings where each element is a code block or a non-code block text from the
     * input string.
     */
    @JvmStatic
    fun splitCodeBlocks(inputMarkdown: String): List<String> {
        val result: MutableList<String> = ArrayList()
        val pattern = Pattern.compile(
            """(?m)^```[a-zA-Z0-9]*\r?\n.*?\r?\n```""",
            Pattern.DOTALL
        )
        val matcher = pattern.matcher(inputMarkdown)
        var start = 0
        while (matcher.find()) {
            result.add(inputMarkdown.substring(start, matcher.start()))
            result.add(matcher.group())
            start = matcher.end()
        }
        result.add(inputMarkdown.substring(start))
        return result.stream().filter(String::isNotBlank).toList()
    }

    @JvmStatic
    fun convertMdToHtml(message: String): String {
        val options = MutableDataSet()
        options.set(Parser.EXTENSIONS, listOf(TablesExtension.create()))
        options.set(HtmlRenderer.SOFT_BREAK, "<br/>")
        val document = Parser.builder(options).build().parse(message)
        return HtmlRenderer.builder(options)
            .nodeRendererFactory(ResponseNodeRenderer.Factory())
            .build()
            .render(document)
    }

    @JvmStatic
    fun convertMdToErrorHtml(message: String): String {
        val options = MutableDataSet()
        options.set(Parser.EXTENSIONS, listOf(TablesExtension.create()))
        options.set(HtmlRenderer.SOFT_BREAK, "<br/>")
        val document = Parser.builder(options).build().parse(message)
        val rendered = HtmlRenderer.builder(options)
            .nodeRendererFactory(ResponseNodeRenderer.Factory())
            .build()
            .render(document)
        val regex = Regex("<p(\\s*[^>]*)>")
        return regex.replace(rendered) { matchResult ->
            val originalAttr = matchResult.groupValues[1]
            // 如果已经有 style，追加 color:red
            if (originalAttr.contains("style=", ignoreCase = true)) {
                // 在原 style 后追加 color:red;
                matchResult.value.replace(Regex("style=\"([^\"]*)\"")) { styleMatch ->
                    val existing = styleMatch.groupValues[1]
                    "style=\"$existing color:red;\""
                }
            } else {
                // 没有 style，直接加上 color:red
                "<p$originalAttr style=\"color:red;\">"
            }
        }
    }

}
