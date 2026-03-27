package com.qihoo.finance.lowcode.codereview.util;

/**
 * LineMarkerUtils
 *
 * @author fengjinfu-jk
 * date 2023/11/13
 * @version 1.0.0
 * @apiNote LineMarkerUtils
 */

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;

import javax.swing.*;

public class LineMarkerUtils {

    public static void addLineMarkup(Editor editor, int offset, Icon icon, String body, AnAction clickAction) {
        Project project = editor.getProject();
        if (project == null) {
            return;
        }

        // 创建自定义图标
        IconRenderer renderer = new IconRenderer(offset, icon, body, clickAction);
        MarkupModel markupModel = editor.getMarkupModel();

        RangeHighlighter[] existingHighlighters = markupModel.getAllHighlighters();
        for (RangeHighlighter highlighter : existingHighlighters) {
            if (renderer.equals(highlighter.getGutterIconRenderer())) {
                // 如果已经存在相同范围的 RangeHighlighter，则不再添加
                return;
            }
        }

        // 创建自定义标记
        RangeHighlighter highlighter = markupModel.addLineHighlighter(Math.max(offset - 1, 0), HighlighterLayer.ERROR + 1, null);

        // 设置标记的类型为自定义图标
        highlighter.setGutterIconRenderer(renderer);
    }

    private static class IconRenderer extends GutterIconRenderer {
        private final Icon icon;
        private final int offset;
        private final String tips;
        private final AnAction clickAction;

        public IconRenderer(int offset, Icon icon, String tips, AnAction clickAction) {
            this.icon = icon;
            this.offset = offset;
            this.tips = tips;
            this.clickAction = clickAction;
        }

        @Override
        public AnAction getClickAction() {
            return clickAction;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof IconRenderer && this.icon.equals(((IconRenderer) obj).icon) && this.offset == ((IconRenderer) obj).offset;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public Icon getIcon() {
            return icon;
        }

        @Override
        public String getTooltipText() {
            return tips;
        }
    }
}
