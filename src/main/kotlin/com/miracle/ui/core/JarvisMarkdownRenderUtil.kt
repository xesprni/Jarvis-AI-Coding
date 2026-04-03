package com.miracle.ui.core

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
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
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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
        return createHtmlPane(project = null, html = html, opaque = opaque)
    }

    fun createHtmlPane(project: Project?, html: String, opaque: Boolean = false): JTextPane {
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
            if (event.eventType != HyperlinkEvent.EventType.ACTIVATED) return@addHyperlinkListener

            val description = event.description.orEmpty()
            if (ChatFileLinkHandler.isJarvisFileLink(description)) {
                if (project != null) {
                    ChatFileLinkHandler.navigate(project, description)
                }
                return@addHyperlinkListener
            }

            when {
                event.url != null -> BrowserUtil.browse(event.url)
                description.startsWith("http://", ignoreCase = true) ||
                    description.startsWith("https://", ignoreCase = true) -> BrowserUtil.browse(description)
            }
        }
        (textPane.caret as? DefaultCaret)?.updatePolicy = DefaultCaret.NEVER_UPDATE
        return textPane
    }

}

internal data class JarvisFileLinkTarget(
    val relativePath: String,
    val line: Int? = null,
    val column: Int? = null,
)

internal object ChatFileLinkHandler {
    private const val JARVIS_FILE_SCHEME = "jarvis-file://"
    private val LOG = Logger.getInstance(ChatFileLinkHandler::class.java)

    fun isJarvisFileLink(rawHref: String?): Boolean {
        return rawHref?.startsWith(JARVIS_FILE_SCHEME, ignoreCase = true) == true
    }

    fun parse(rawHref: String?): JarvisFileLinkTarget? {
        val href = rawHref?.takeIf(::isJarvisFileLink) ?: return null
        val rawTarget = href.substring(JARVIS_FILE_SCHEME.length).substringBefore("#")
        val pathAndQuery = rawTarget.split("?", limit = 2)
        val relativePath = decodeComponent(pathAndQuery[0]).replace('\\', '/')
        if (relativePath.isBlank()) return null
        if (relativePath.startsWith("/") || relativePath.startsWith("\\")) return null
        if (relativePath.contains('\u0000')) return null
        if (WINDOWS_ABSOLUTE_PATH.matches(relativePath)) return null

        val query = parseQuery(pathAndQuery.getOrElse(1) { "" })
        val line = parsePositiveInt(query["line"]) ?: if ("line" in query) return null else null
        val column = parsePositiveInt(query["column"]) ?: if ("column" in query) return null else null
        if (column != null && line == null) return null

        return JarvisFileLinkTarget(relativePath = relativePath, line = line, column = column)
    }

    fun resolveProjectFile(projectBasePath: String?, target: JarvisFileLinkTarget): Path? {
        if (projectBasePath.isNullOrBlank()) return null

        val basePath = runCatching { Paths.get(projectBasePath).normalize().toRealPath() }.getOrNull() ?: return null
        val candidatePath = runCatching { basePath.resolve(target.relativePath).normalize() }.getOrNull() ?: return null
        if (!candidatePath.startsWith(basePath)) return null

        val realPath = runCatching { candidatePath.toRealPath() }.getOrNull() ?: return null
        if (!realPath.startsWith(basePath)) return null
        if (!Files.isRegularFile(realPath)) return null
        return realPath
    }

    fun navigate(project: Project, rawHref: String): Boolean {
        val target = parse(rawHref)
        if (target == null) {
            LOG.warn("Rejected malformed jarvis-file link: $rawHref")
            return false
        }

        val resolvedPath = resolveProjectFile(project.basePath, target)
        if (resolvedPath == null) {
            LOG.warn("Rejected jarvis-file link outside project or missing file: $rawHref")
            return false
        }

        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(resolvedPath.toString())
        if (virtualFile == null || !virtualFile.isValid) {
            LOG.warn("Failed to resolve virtual file for jarvis-file link: $rawHref")
            return false
        }

        if (target.line == null) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        } else {
            OpenFileDescriptor(
                project,
                virtualFile,
                target.line - 1,
                (target.column ?: 1) - 1,
            ).navigate(true)
        }
        return true
    }

    private fun parseQuery(rawQuery: String): Map<String, String> {
        if (rawQuery.isBlank()) return emptyMap()
        return rawQuery.split("&")
            .asSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { entry ->
                val parts = entry.split("=", limit = 2)
                val key = decodeComponent(parts[0]).trim()
                if (key.isBlank()) return@mapNotNull null
                val value = decodeComponent(parts.getOrElse(1) { "" }).trim()
                key to value
            }
            .toMap()
    }

    private fun decodeComponent(value: String): String {
        return URLDecoder.decode(value, StandardCharsets.UTF_8).trim()
    }

    private fun parsePositiveInt(value: String?): Int? {
        return value?.toIntOrNull()?.takeIf { it > 0 }
    }

    private val WINDOWS_ABSOLUTE_PATH = Regex("^[A-Za-z]:[/\\\\].*")
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
