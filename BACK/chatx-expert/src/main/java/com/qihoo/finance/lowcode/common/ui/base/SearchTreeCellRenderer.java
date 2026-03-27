package com.qihoo.finance.lowcode.common.ui.base;

import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TreeSpeedSearch;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JTreeToolbarUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.Objects;

/**
 * SearchTreeCellRenderer
 *
 * @author fengjinfu-jk
 * date 2023/9/26
 * @version 1.0.0
 * @apiNote SearchTreeCellRenderer
 */
@Getter
public abstract class SearchTreeCellRenderer extends NodeRenderer {
    @Setter
    protected JTextField searchKeyTxt;
    protected DefaultMutableTreeNode currNode;
    private final Checkbox filter;
    private static final Icon COLLECT = Icons.scaleToWidth(Icons.COLLECT, 13);
    private static final Color HIGHLIGHT = new JBColor(Color.BLACK, Color.YELLOW);
    private static final Color HIGHLIGHT_FOREGROUND = JBColor.BLACK;

    public SearchTreeCellRenderer(JTree tree) {
        this.searchKeyTxt = new JTextField();
        this.filter = new Checkbox();
        setOpaque(false);

        // search
        new TreeSpeedSearch(tree, path -> {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            return node.toString();
        });
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree tree, @NlsSafe Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        currNode = node;
        // search render
        searchRender(tree, node);
        // custom render
        setTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        if (node instanceof PlaceTextNode placeTextNode) {
            String placeText = placeTextNode.getDescription();
            if (StringUtils.isNotEmpty(placeText)) {
//                append("  " + placeText, placeTextNode.getAttributes(), false);
                FontMetrics fontMetrics = tree.getFontMetrics(tree.getFont());
                int availableWidth = getAvailableWidth(tree, node, fontMetrics);
                String truncatedText = truncateTextToFit(placeText, availableWidth, fontMetrics);
                append("  " + truncatedText, placeTextNode.getAttributes(), false);
            }
        }
    }

    private int getAvailableWidth(JTree tree, DefaultMutableTreeNode node, FontMetrics fontMetrics) {
        int treeWidth = tree.getWidth();
        int textWidth = fontMetrics.stringWidth(node.toString());
        int depth = 0;
        while(node.getParent() != null) {
            node = (DefaultMutableTreeNode) node.getParent();
            depth++;
        }
        return treeWidth - depth * 18 - textWidth - 52;
    }

    private String truncateTextToFit(String text, int maxWidth, FontMetrics fontMetrics) {
        int textWidth = fontMetrics.stringWidth(text);
        if (textWidth <= maxWidth) {
            return text; // No need to truncate
        }

        String ellipsis = "...";
        int ellipsisWidth = fontMetrics.stringWidth(ellipsis);

        for (int i = text.length() - 1; i > 0; i--) {
            String truncatedText = text.substring(0, i);
            if (fontMetrics.stringWidth(truncatedText) + ellipsisWidth <= maxWidth) {
                return truncatedText + ellipsis; // Add ellipsis
            }
        }

        return ellipsis; // If text is too short, show only ellipsis
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int x = getIcon() != null ? getIcon().getIconWidth() + getIconTextGap() : 0;
        int y = (getHeight() - COLLECT.getIconHeight()) / 2;
        int width = this.getSize().width;

        if (currNode instanceof FilterableTreeNode filterNode && filterNode.isCollect()) {
            COLLECT.paintIcon(this, g, width - 2 * x, y);
        }
    }

    private void searchRender(@NotNull JTree tree, DefaultMutableTreeNode node) {
        String key = searchKeyTxt.getText().trim();
        if (JTreeToolbarUtils.INPUT_TIP.equalsIgnoreCase(key) || StringUtils.EMPTY.equalsIgnoreCase(key)) {
            return;
        }

        String valueStr = Objects.nonNull(node) ? StringUtils.defaultString(node.toString()) : StringUtils.EMPTY;
        if (StringUtils.isNotEmpty(key) && valueStr.toLowerCase().contains(key.toLowerCase())) {
            setHtmlHighlight(node, key);
        }
    }

    private void setHtmlHighlight(DefaultMutableTreeNode c, String searchKey) {
        String text = c.toString();
        if (StringUtils.isEmpty(text)) return;

        String lowCaseText = text.toLowerCase();
        String lowCaseSearchKey = searchKey.toLowerCase();

        int highlightIndex = lowCaseText.indexOf(lowCaseSearchKey);
        String prefix = text.substring(0, highlightIndex);
        String highlight = text.substring(highlightIndex, highlightIndex + searchKey.length());
        String suffix = text.substring(highlightIndex + searchKey.length());

        clear();
        if (StringUtils.isNotEmpty(prefix)) {
            append(prefix, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        if (StringUtils.isNotEmpty(highlight)) {
            TextAttributes textAttributes = new TextAttributes();
            // fixme: background 不生效
            textAttributes.setBackgroundColor(HIGHLIGHT);
            textAttributes.setForegroundColor(HIGHLIGHT);
            textAttributes.setFontType(Font.BOLD);
//            textAttributes.setForegroundColor(HIGHLIGHT_FOREGROUND);
            append(highlight, SimpleTextAttributes.fromTextAttributes(textAttributes));
        }

        if (StringUtils.isNotEmpty(suffix)) {
            append(suffix, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
    }

    public abstract void setTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus);
}
