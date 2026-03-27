package com.qihoo.finance.lowcode.aiquestion.ui.search.action;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.MarkupModel;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.util.TextRange;
import com.qihoo.finance.lowcode.common.util.Icons;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;

/**
 * LuneMarkerAction
 *
 * @author fengjinfu-jk
 * date 2024/8/7
 * @version 1.0.0
 * @apiNote LuneMarkerAction
 */
public class LineMarkerAction extends DumbAwareAction implements Disposable {
    public static final String ACTION_ID = "Chatx.lineMarker.askAI";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (Objects.isNull(editor)) return;

        String content = getLineMarkerContent(editor);
        System.out.println("Ask Jarvis" + content);
    }

    public static void registerAskAILineMarker() {
        AnAction action = ActionManager.getInstance().getAction(ACTION_ID);
        if (Objects.isNull(action)) return;
        if (!(action instanceof LineMarkerAction)) return;

        AIRenderer aiRenderer = new AIRenderer(Icons.LOGO_ROUND13, "Ask Jarvis", action);

        EditorFactory.getInstance()
                .getEventMulticaster()
                .addEditorMouseListener(new EditorMouseListener() {
                    int lastLine = -1;

                    @Override
                    public void mouseReleased(@NotNull EditorMouseEvent event) {
                        EditorMouseListener.super.mouseReleased(event);
                        int lineNumber = event.getEditor().getDocument().getLineNumber(event.getOffset());
                        if (lineNumber == lastLine) {
                            return;
                        }
                        lastLine = lineNumber;
                        addLineMarker(aiRenderer, event.getEditor());
                    }

                    @Override
                    public void mouseClicked(@NotNull EditorMouseEvent event) {
                        EditorMouseListener.super.mouseClicked(event);
                        int lineNumber = event.getEditor().getDocument().getLineNumber(event.getOffset());
                        if (lineNumber == lastLine) {
                            return;
                        }
                        lastLine = lineNumber;
                        addLineMarker(aiRenderer, event.getEditor());
                    }
                }, (Disposable) action);
    }

    private static void addLineMarker(AIRenderer aiRenderer, @NotNull Editor editor) {
        boolean selected = StringUtils.isNotBlank(editor.getSelectionModel().getSelectedText());
        int line = selected ? editor.getDocument().getLineNumber(editor.getSelectionModel().getSelectionStart()) :
                editor.getDocument().getLineNumber(editor.getCaretModel().getCurrentCaret().getOffset());

        boolean hadLineMarker = false;
        MarkupModel markupModel = editor.getMarkupModel();
        RangeHighlighter[] existingHighlighters = markupModel.getAllHighlighters();
        for (RangeHighlighter highlighter : existingHighlighters) {
            if (highlighter.getGutterIconRenderer() instanceof AIRenderer) {
                int oldLine = editor.getDocument().getLineNumber(highlighter.getStartOffset());
                if (oldLine != line) {
                    markupModel.removeHighlighter(highlighter);
                } else {
                    // 如果已经存在相同范围的 RangeHighlighter，则不再添加
                    hadLineMarker = true;
                }
            }
        }

        if (hadLineMarker) return;

        String content = getLineMarkerContent(editor);
        if (StringUtils.isBlank(content)) return;

        // 创建自定义标记
        RangeHighlighter highlighter = markupModel.addLineHighlighter(line, HighlighterLayer.HYPERLINK, null);

        // 设置标记的类型为自定义图标
        highlighter.setGutterIconRenderer(aiRenderer);
    }

    private static String getLineMarkerContent(@NotNull Editor editor) {
        String selectedText = editor.getSelectionModel().getSelectedText();
        if (StringUtils.isBlank(selectedText)) {
            Document document = editor.getDocument();
            int line = document.getLineNumber(editor.getCaretModel().getCurrentCaret().getOffset());
            selectedText = document.getText(new TextRange(document.getLineStartOffset(line), document.getLineEndOffset(line)));
        }

        return selectedText;
    }

    @Override
    public void dispose() {

    }


    private static class AIRenderer extends GutterIconRenderer {
        private final Icon icon;
        private final String tips;
        private final AnAction clickAction;

        public AIRenderer(Icon icon, String tips, AnAction clickAction) {
            this.icon = icon;
            this.tips = tips;
            this.clickAction = clickAction;
        }

        @Override
        public AnAction getClickAction() {
            return clickAction;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof AIRenderer && this.icon.equals(((AIRenderer) obj).icon);
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public @NotNull Icon getIcon() {
            return icon;
        }

        @Override
        public String getTooltipText() {
            return tips;
        }
    }
}
