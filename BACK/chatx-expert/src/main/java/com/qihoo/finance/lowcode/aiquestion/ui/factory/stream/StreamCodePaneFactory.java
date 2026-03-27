package com.qihoo.finance.lowcode.aiquestion.ui.factory.stream;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.qihoo.finance.lowcode.aiquestion.ui.QuestionPanel;
import com.qihoo.finance.lowcode.aiquestion.ui.component.HorizontalScrollBarEditor;
import com.qihoo.finance.lowcode.aiquestion.ui.component.SoftWrapHorizontalScrollBarEditor;
import com.qihoo.finance.lowcode.aiquestion.util.ColorUtil;
import com.qihoo.finance.lowcode.common.util.ApplicationUtil;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class StreamCodePaneFactory implements StreamRender {

    private final String codeType;
    @NonNull
    private String content;
    private final AtomicInteger atomicPos;
    private final StreamRender parentRender;
    private final static String CODE_IDENTIFIER = "```";

    @Setter
    @Getter
    private boolean isAgent = false;
    @Setter
    @Getter
    private JButton expandButton;

    private int pos = -1;
    private Project project;
    @Getter
    private EditorTextField textField;
    private QuestionPanel questionPanel;

    public EditorTextField create() {
        project = ApplicationUtil.findCurrentProject();
        questionPanel = ProjectUtils.getCurrProject().getService(QuestionPanel.class);
        if (codeType.startsWith(StreamCodePanelFactory.AGENT_TYPE_START)) {
            textField = new SoftWrapHorizontalScrollBarEditor(project, PlainTextFileType.INSTANCE);
            textField.setOneLineMode(false);
            textField.setViewer(true);
            textField.setBackground(ColorUtil.getCodeContentBackground());
            return textField;
        }
        FileType fileType = null;
        try {
            if (StringUtils.isNotBlank(codeType)) {
                String fileExt = codeType;
                if ("python".equalsIgnoreCase(codeType)) {
                    fileExt = "py";
                }
                fileType = FileTypeManager.getInstance().getFileTypeByExtension(fileExt);
                if (fileType instanceof UnknownFileType) {
                    fileType = FileTypeManager.getInstance().getFileTypeByExtension("java");
                }
            }
        } catch (Exception ignored) {
        }
        if (fileType == null || fileType instanceof UnknownFileType) {
            fileType = PlainTextFileType.INSTANCE;
        }
        textField = new HorizontalScrollBarEditor(project, fileType);
        textField.setOneLineMode(false);
        textField.setViewer(true);
        textField.setBackground(ColorUtil.getCodeContentBackground());
        return textField;
    }

    @Override
    public void render() {
        if (pos == -1) {
            int codePos = atomicPos.get() + CODE_IDENTIFIER.length();
            int newLineIndex = content.indexOf('\n', codePos);
            // 找不到换行符，等待下次内容一起加载
            if (newLineIndex == -1) {
                return;
            }
            pos = newLineIndex + 1;
        }
        // 获取代码块内容
        int codeEndIndex = getCodeEndIndex(content, pos);
        String codeToAppend = content.substring(pos, codeEndIndex);
        pos = codeEndIndex;
        WriteCommandAction.runWriteCommandAction(project, () -> {
            Document document = textField.getDocument();
            String cleanCode = codeToAppend.replaceAll("\r\n", "\n").replaceAll("\r", "\n");
            document.insertString(document.getTextLength(), cleanCode);
        });
        codeEndIndex = content.indexOf(CODE_IDENTIFIER, pos);
        if (codeEndIndex != -1) {
            codeEndIndex += CODE_IDENTIFIER.length();
            if (codeEndIndex < content.length() && content.charAt(codeEndIndex) == '\n') {
                codeEndIndex++;
            }
            atomicPos.set(codeEndIndex);
            parentRender.resumeRender();

            // 结束并自动收起<agent>块
            if (isAgent) {
                textField.setVisible(false);
                if (Objects.nonNull(expandButton)) expandButton.setIcon(Icons.scaleToWidth(Icons.AI_EXPAND, 16));
            }
        } else if (pos >= content.length()) {
            parentRender.resumeRender();
        }
        if (Objects.nonNull(questionPanel)) {
            questionPanel.scrollBottom(false);
        }
    }

    private static int getCodeEndIndex(String content, int pos) {
        int codeEndIndex = content.indexOf(CODE_IDENTIFIER, pos);
        if (codeEndIndex == -1) {
            codeEndIndex = content.length();
        }
        return codeEndIndex;
    }

    @Override
    public void flushRender(String content) {
        this.content = content;
        render();
    }

    @Override
    public void repaint() {
        parentRender.repaint();
    }

    @Override
    public void resumeRender() {
        // 解决栈溢出问题
        new SwingWorker<StreamRender, String>() {
            @Override
            protected StreamRender doInBackground() throws Exception {
                return parentRender;
            }

            @Override
            protected void done() {
                parentRender.resumeRender();
            }
        }.execute();
//        parentRender.resumeRender();
    }
}
