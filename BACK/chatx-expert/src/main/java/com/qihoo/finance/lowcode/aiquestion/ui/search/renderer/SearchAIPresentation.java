package com.qihoo.finance.lowcode.aiquestion.ui.search.renderer;

import com.intellij.codeInsight.hints.presentation.InputHandler;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCustomElementRenderer;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.content.Content;
import com.qihoo.finance.lowcode.aiquestion.util.PsiUtils;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowPanel;
import com.qihoo.finance.lowcode.smartconversation.panels.SmartToolWindowTabPanel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.UUID;

/**
 * SearchAIPresentation
 *
 * @author fengjinfu-jk
 * date 2024/8/6
 * @version 1.0.0
 * @apiNote JarvisPresentation
 */
@Slf4j
public class SearchAIPresentation implements EditorCustomElementRenderer, InputHandler {
    private final Editor editor;
    private final Project project;
    private final int startOffset;
    private final String exceptionClassName;

    public SearchAIPresentation(Editor editor, Project project, int starOffset, String exceptionClassName) {
        this.editor = editor;
        this.project = project;
        this.startOffset = starOffset;
        this.exceptionClassName = exceptionClassName;
    }

    private String getErrorStacktrace(Document document, int startOffset, int line) {
        try {
            String errorHeader = document.getText(new TextRange(startOffset, document.getLineEndOffset(line)));
            StringBuilder sb = new StringBuilder(errorHeader);
            ++line;

            while (line < document.getLineCount()) {
                String lineContent = document.getText(new TextRange(document.getLineStartOffset(line), document.getLineEndOffset(line)));
                if (!lineContent.trim().startsWith("at ") && !lineContent.trim().startsWith("Caused by") && !lineContent.trim().startsWith("...")) {
                    break;
                }

                sb.append("\n");
                sb.append(lineContent);
                ++line;
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("getErrorStacktrace error ", e);
            return StringUtils.EMPTY;
        }
    }

    public void mouseClicked(@NotNull MouseEvent mouseEvent, @NotNull Point point) {
        int line = this.editor.getDocument().getLineNumber(this.startOffset);
        String errorInformation = this.getErrorStacktrace(this.editor.getDocument(), this.startOffset, line);
        int maxLine = errorInformation.split("\n").length + line;
        String codeContext = PsiUtils.findErrorLineContent(this.project, this.editor, line, maxLine);
        // search ai
        ChatXToolWindowFactory.showFirstTab();
        Content content = ChatXToolWindowFactory.getToolWindow().getContentManager().getSelectedContent();
        if (content == null) {
            NotifyUtils.notify("快捷指令前置验证失败", NotificationType.WARNING);
            return;
        }
        if (content.getComponent() instanceof SmartToolWindowPanel smartToolWindowPanel) {
            String prompt = String.format("我正在遇到一个程序运行错误。我会为你提供【错误堆栈信息】和【相关源代码】。请你通过以下步骤帮我解决问题：\n" +
                    "\n" +
                    "1. **定位错误点**：准确指出报错发生的行号以及直接导致崩溃的变量或逻辑。\n" +
                    "2. **原因分析**：深入浅出地解释为什么会报错（是语法错误、逻辑漏洞、环境问题、还是异步处理不当等）。\n" +
                    "3. **解决方案**：\n" +
                    "   - 提供一份**修改后的代码示例**。\n" +
                    "   - 如果有多种修复方式（如临时修复 vs 根本解决），请分别说明优缺点。\n" +
                    "4. **预防建议**：说明在未来的开发中，应如何通过代码规范、测试或类型检查来避免同类问题。\n" +
                    "\n" +
                    "# Context\n" +
                    "### 1. 错误堆栈 (Error Stack Trace)\n" +
                    "```text\n%s\n```\n" +
                    "\n" +
                    "### 2. 相关代码 (Code Snippet)\n" +
                    "%s", errorInformation, codeContext);
            String taskId = UUID.randomUUID().toString().replace("-", "");
            SmartToolWindowTabPanel smartToolWindowTabPanel = new SmartToolWindowTabPanel(project, taskId);
            smartToolWindowPanel.getChatTabbedPane().addNewTab(smartToolWindowTabPanel, taskId, "");
            smartToolWindowPanel.getChatTabbedPane().trySwitchTab(taskId);
            smartToolWindowTabPanel.handleSubmit(prompt);
        }
    }

    public void mouseExited() {
        ((EditorImpl) this.editor).setCustomCursor(this, Cursor.getPredefinedCursor(2));
    }

    public void mouseMoved(@NotNull MouseEvent mouseEvent, @NotNull Point point) {
        ((EditorImpl) this.editor).setCustomCursor(this, Cursor.getPredefinedCursor(12));
    }

    public int calcWidthInPixels(@NotNull Inlay inlay) {
        return Icons.CONSOLE_ICON.getIconWidth();
    }

    public int calcHeightInPixels(@NotNull Inlay inlay) {
        return Icons.CONSOLE_ICON.getIconHeight();
    }

    public void paint(@NotNull Inlay inlay, @NotNull Graphics g, @NotNull Rectangle r, @NotNull TextAttributes textAttributes) {
        Color color = EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.READONLY_FRAGMENT_BACKGROUND_COLOR);
        Icon consoleIcon;
        if (color == null) {
            consoleIcon = Icons.CONSOLE_ICON;
        } else {
            consoleIcon = Icons.CONSOLE_ICON;
        }

        int curX = r.x + r.width / 2 - consoleIcon.getIconWidth() / 2;
        int curY = r.y + r.height / 2 - consoleIcon.getIconHeight() / 2;
        if (curX >= 0 && curY >= 0) {
            consoleIcon.paintIcon(inlay.getEditor().getComponent(), g, curX, curY);
        }
    }
}
