package com.qihoo.finance.lowcode.smartconversation.service;

import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ext.tables.*;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.util.data.DataHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ResponseNodeRenderer implements NodeRenderer {

  @Override
  public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
    return Set.of(
        new NodeRenderingHandler<>(Paragraph.class, this::renderParagraph),
        new NodeRenderingHandler<>(Code.class, this::renderCode),
        new NodeRenderingHandler<>(CodeBlock.class, this::renderCodeBlock),
        new NodeRenderingHandler<>(BulletListItem.class, this::renderBulletListItem),
        new NodeRenderingHandler<>(Heading.class, this::renderHeading),
        new NodeRenderingHandler<>(OrderedListItem.class, this::renderOrderedListItem),
        new NodeRenderingHandler<>(TableBlock.class, this::renderTable),
        new NodeRenderingHandler<>(TableHead.class, this::renderTableHead),
        new NodeRenderingHandler<>(TableBody.class, this::renderTableBody),
        new NodeRenderingHandler<>(TableRow.class, this::renderTableRow),
        new NodeRenderingHandler<>(TableCell.class, this::renderTableCell)
    );
  }

  private void renderCodeBlock(CodeBlock node, NodeRendererContext context, HtmlWriter html) {
    html.attr("style", "white-space: pre-wrap;");
    context.delegateRender();
  }

  private void renderHeading(Heading node, NodeRendererContext context, HtmlWriter html) {
    html.attr("style", "margin-top: 8px; margin-bottom: 4px;");
    context.delegateRender();
  }

  private void renderParagraph(Paragraph node, NodeRendererContext context, HtmlWriter html) {
    if (node.getParent() instanceof BulletListItem || node.getParent() instanceof OrderedListItem) {
      html.attr("style", "margin: 0; padding:0;");
    } else {
      html.attr("style", "margin-top: 4px; margin-bottom: 4px;");
    }
    context.delegateRender();
  }

  private void renderCode(Code node, NodeRendererContext context, HtmlWriter html) {
    html.attr("style", "color: " + ColorUtil.toHex(new JBColor(0x00627A, 0xCC7832)));
    context.delegateRender();
  }

  private void renderBulletListItem(
      BulletListItem node,
      NodeRendererContext context,
      HtmlWriter html) {
    html.attr("style", "margin-bottom: 4px;");
    context.delegateRender();
  }

  private void renderOrderedListItem(
      OrderedListItem node,
      NodeRendererContext context,
      HtmlWriter html) {
    html.attr("style", "margin-bottom: 4px;");
    context.delegateRender();
  }

  private void renderTable(TableBlock node, NodeRendererContext context, HtmlWriter html) {
    // Swing HTMLEditorKit 不支持 overflow-x，但我们可以通过设置表格为固定宽度
    // 并让其超出容器来配合外层 JTextPane 的 getScrollableTracksViewportWidth
    // 使用传统 HTML 表格属性以兼容 Swing HTMLEditorKit
    html.attr("border", "1");
    html.attr("cellpadding", "6");
    html.attr("cellspacing", "0");
    // 不设置 width 属性，让表格保持内容的实际宽度
    html.attr("style", "margin: 8px 0; border-color: " +
        ColorUtil.toHex(new JBColor(0xD0D0D0, 0x3C3F41)) + "; border-collapse: collapse;");
    context.delegateRender();
  }

  private void renderTableHead(TableHead node, NodeRendererContext context, HtmlWriter html) {
    html.attr("style", "background-color: " + ColorUtil.toHex(new JBColor(0xF5F5F5, 0x2B2B2B)) + ";");
    context.delegateRender();
  }

  private void renderTableBody(TableBody node, NodeRendererContext context, HtmlWriter html) {
    context.delegateRender();
  }

  private void renderTableRow(TableRow node, NodeRendererContext context, HtmlWriter html) {
    html.attr("style", "border-bottom: 1px solid " + 
        ColorUtil.toHex(new JBColor(0xE0E0E0, 0x3C3F41)) + ";");
    context.delegateRender();
  }

  private void renderTableCell(TableCell node, NodeRendererContext context, HtmlWriter html) {
    // 使用 nowrap 防止单元格内容换行，配合 JScrollPane 实现横向滚动
    html.attr("nowrap", "nowrap");
    html.attr("style", "padding: 6px 12px; border: 1px solid " +
        ColorUtil.toHex(new JBColor(0xD0D0D0, 0x3C3F41)) + "; text-align: left; white-space: nowrap;");
    context.delegateRender();
  }

  public static class Factory implements NodeRendererFactory {

    @NotNull
    @Override
    public NodeRenderer apply(@NotNull DataHolder options) {
      return new ResponseNodeRenderer();
    }
  }
}