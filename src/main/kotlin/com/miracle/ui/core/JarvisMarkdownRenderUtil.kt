package com.miracle.ui.core

import com.intellij.ide.BrowserUtil
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.vladsch.flexmark.ast.BulletListItem
import com.vladsch.flexmark.ast.Code
import com.vladsch.flexmark.ast.CodeBlock
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.OrderedListItem
import com.vladsch.flexmark.ast.Paragraph
import com.vladsch.flexmark.ext.tables.TableBlock
import com.vladsch.flexmark.ext.tables.TableBody
import com.vladsch.flexmark.ext.tables.TableCell
import com.vladsch.flexmark.ext.tables.TableHead
import com.vladsch.flexmark.ext.tables.TableRow
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.HtmlWriter
import com.vladsch.flexmark.html.renderer.NodeRenderer
import com.vladsch.flexmark.html.renderer.NodeRendererContext
import com.vladsch.flexmark.html.renderer.NodeRendererFactory
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.DataHolder
import com.vladsch.flexmark.util.data.MutableDataSet
import java.awt.Color
import javax.swing.JTextPane
import javax.swing.event.HyperlinkEvent
import javax.swing.text.DefaultCaret
import javax.swing.text.html.StyleSheet

object JarvisMarkdownRenderUtil {

    private fun parserOptions(): MutableDataSet {
        return MutableDataSet().apply {
            set(Parser.EXTENSIONS, listOf(TablesExtension.create()))
            set(HtmlRenderer.SOFT_BREAK, "<br/>")
        }
    }

    fun convertMarkdownToHtml(markdown: String): String {
        val options = parserOptions()
        val document = Parser.builder(options).build().parse(markdown)
        return HtmlRenderer.builder(options)
            .nodeRendererFactory(JarvisResponseNodeRenderer.Factory())
            .build()
            .render(document)
    }

    fun convertMarkdownToErrorHtml(markdown: String): String {
        val rendered = convertMarkdownToHtml(markdown)
        val regex = Regex("<p(\\s*[^>]*)>")
        return regex.replace(rendered) { matchResult ->
            val attrs = matchResult.groupValues[1]
            if (attrs.contains("style=", ignoreCase = true)) {
                matchResult.value.replace(Regex("style=\"([^\"]*)\"")) { styleMatch ->
                    val existing = styleMatch.groupValues[1]
                    "style=\"$existing color:red;\""
                }
            } else {
                "<p$attrs style=\"color:red;\">"
            }
        }
    }

    fun createHtmlPane(html: String, opaque: Boolean = false): JTextPane {
        val textPane = JTextPane()
        textPane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, true)
        val editorKit = HTMLEditorKitBuilder().withWordWrapViewFactory().build()
        val styleSheet: StyleSheet = editorKit.styleSheet
        styleSheet.addRule("body { margin: 0; overflow-wrap: anywhere; word-wrap: break-word; }")
        styleSheet.addRule("p, li, td, th { overflow-wrap: anywhere; word-wrap: break-word; }")
        styleSheet.addRule("pre { white-space: pre-wrap; word-wrap: break-word; overflow-wrap: anywhere; }")
        styleSheet.addRule("code { white-space: pre-wrap; word-wrap: break-word; }")
        textPane.editorKit = editorKit
        textPane.contentType = "text/html"
        textPane.isEditable = false
        textPane.text = html
        textPane.isOpaque = opaque
        textPane.addHyperlinkListener { event ->
            if (event.eventType == HyperlinkEvent.EventType.ACTIVATED && event.url != null) {
                BrowserUtil.browse(event.url)
            }
        }
        (textPane.caret as? DefaultCaret)?.updatePolicy = DefaultCaret.NEVER_UPDATE
        return textPane
    }
}

private class JarvisResponseNodeRenderer : NodeRenderer {
    override fun getNodeRenderingHandlers(): Set<NodeRenderingHandler<*>> {
        return setOf(
            NodeRenderingHandler(Paragraph::class.java, this::renderParagraph),
            NodeRenderingHandler(Code::class.java, this::renderCode),
            NodeRenderingHandler(CodeBlock::class.java, this::renderCodeBlock),
            NodeRenderingHandler(BulletListItem::class.java, this::renderBulletListItem),
            NodeRenderingHandler(Heading::class.java, this::renderHeading),
            NodeRenderingHandler(OrderedListItem::class.java, this::renderOrderedListItem),
            NodeRenderingHandler(TableBlock::class.java, this::renderTable),
            NodeRenderingHandler(TableHead::class.java, this::renderTableHead),
            NodeRenderingHandler(TableBody::class.java, this::renderTableBody),
            NodeRenderingHandler(TableRow::class.java, this::renderTableRow),
            NodeRenderingHandler(TableCell::class.java, this::renderTableCell),
        )
    }

    private fun renderCodeBlock(node: CodeBlock, context: NodeRendererContext, html: HtmlWriter) {
        html.attr("style", "white-space: pre-wrap;")
        context.delegateRender()
    }

    private fun renderHeading(node: Heading, context: NodeRendererContext, html: HtmlWriter) {
        html.attr("style", "margin-top: 8px; margin-bottom: 4px;")
        context.delegateRender()
    }

    private fun renderParagraph(node: Paragraph, context: NodeRendererContext, html: HtmlWriter) {
        if (node.parent is BulletListItem || node.parent is OrderedListItem) {
            html.attr("style", "margin: 0; padding:0;")
        } else {
            html.attr("style", "margin-top: 4px; margin-bottom: 4px;")
        }
        context.delegateRender()
    }

    private fun renderCode(node: Code, context: NodeRendererContext, html: HtmlWriter) {
        html.attr("style", "color: ${ColorUtil.toHex(JBColor(0x00627A, 0xCC7832))}")
        context.delegateRender()
    }

    private fun renderBulletListItem(node: BulletListItem, context: NodeRendererContext, html: HtmlWriter) {
        html.attr("style", "margin-bottom: 4px;")
        context.delegateRender()
    }

    private fun renderOrderedListItem(node: OrderedListItem, context: NodeRendererContext, html: HtmlWriter) {
        html.attr("style", "margin-bottom: 4px;")
        context.delegateRender()
    }

    private fun renderTable(node: TableBlock, context: NodeRendererContext, html: HtmlWriter) {
        html.attr("border", "1")
        html.attr("cellpadding", "6")
        html.attr("cellspacing", "0")
        html.attr(
            "style",
            "margin: 8px 0; border-color: ${ColorUtil.toHex(JBColor(0xD0D0D0, 0x3C3F41))}; border-collapse: collapse;",
        )
        context.delegateRender()
    }

    private fun renderTableHead(node: TableHead, context: NodeRendererContext, html: HtmlWriter) {
        html.attr("style", "background-color: ${ColorUtil.toHex(JBColor(0xF5F5F5, 0x2B2B2B))};")
        context.delegateRender()
    }

    private fun renderTableBody(node: TableBody, context: NodeRendererContext, html: HtmlWriter) {
        context.delegateRender()
    }

    private fun renderTableRow(node: TableRow, context: NodeRendererContext, html: HtmlWriter) {
        html.attr("style", "border-bottom: 1px solid ${ColorUtil.toHex(JBColor(0xE0E0E0, 0x3C3F41))};")
        context.delegateRender()
    }

    private fun renderTableCell(node: TableCell, context: NodeRendererContext, html: HtmlWriter) {
        html.attr("nowrap", "nowrap")
        html.attr(
            "style",
            "padding: 6px 12px; border: 1px solid ${ColorUtil.toHex(JBColor(0xD0D0D0, 0x3C3F41))}; text-align: left; white-space: nowrap;",
        )
        context.delegateRender()
    }

    class Factory : NodeRendererFactory {
        override fun apply(options: DataHolder): NodeRenderer {
            return JarvisResponseNodeRenderer()
        }
    }
}
