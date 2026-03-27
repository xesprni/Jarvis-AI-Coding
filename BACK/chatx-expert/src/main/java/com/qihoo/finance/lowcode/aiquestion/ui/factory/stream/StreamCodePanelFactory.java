package com.qihoo.finance.lowcode.aiquestion.ui.factory.stream;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.aiquestion.ui.factory.ImgButtonFactory;
import com.qihoo.finance.lowcode.aiquestion.util.ChatUtil;
import com.qihoo.finance.lowcode.aiquestion.util.ColorUtil;
import com.qihoo.finance.lowcode.aiquestion.util.EditorUtil;
import com.qihoo.finance.lowcode.aiquestion.util.GenerateTestUtil;
import com.qihoo.finance.lowcode.common.constants.CompletionType;
import com.qihoo.finance.lowcode.common.constants.QuestionType;
import com.qihoo.finance.lowcode.common.entity.enums.CompletionStatus;
import com.qihoo.finance.lowcode.common.ui.base.RoundedPanel;
import com.qihoo.finance.lowcode.common.util.ApplicationUtil;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.console.mysql.SQLEditorManager;
import com.qihoo.finance.lowcode.console.mysql.editor.SQLDataEditor;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class StreamCodePanelFactory implements StreamRender {

    private static final Logger LOG = Logger.getInstance(StreamCodePanelFactory.class);
    private final String codeType;
    @NonNull
    private String content;
    private final AtomicReference<String> contentRefer = new AtomicReference<String>();
    private final AtomicInteger atomicPos;
    private final StreamRender parentRender;
    private final QuestionType questionType;

    private final static ImgButtonFactory imgButtonFactory = new ImgButtonFactory();
    private final static String CODE_IDENTIFIER = "```";
    public final static String AGENT_TYPE_START = "<agent>";
    private final static ExecutorService executorService = Executors.newFixedThreadPool(1);

    @Getter
    private StreamCodePaneFactory codePaneFactory;
    private EditorTextField editorTextField;
    @Getter
    private JPanel codePanel;

    public JPanel create() {
        this.contentRefer.set(content);
        codePanel = new RoundedPanel(ColorUtil.getCodeContentBackground(), 5);
        codePanel.setLayout(new BorderLayout());
        Border emptyBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        codePanel.setBorder(emptyBorder);

        codePaneFactory = new StreamCodePaneFactory(codeType, content, atomicPos, this);
        editorTextField = codePaneFactory.create();
        codePanel.add(editorTextField.getComponent(), BorderLayout.CENTER);
        JPanel headerPanel = createHeaderPanel(codeType, atomicPos, ColorUtil.getCodeTitleBackground());
        codePanel.add(headerPanel, BorderLayout.NORTH);
        codePanel.revalidate();
        codePanel.repaint();

        return codePanel;
    }

    @Override
    public void render() {
        codePaneFactory.render();
    }

    @Override
    public void flushRender(String content) {
        this.content = content;
        this.contentRefer.set(content);
        codePaneFactory.flushRender(content);
    }

    @Override
    public void resumeRender() {
        parentRender.resumeRender();
    }

    @Override
    public void repaint() {
        parentRender.repaint();
    }

    private JPanel createHeaderPanel(String codeType, AtomicInteger atomicPos, Color backgroundColor) {
        JPanel headerPanel = new RoundedPanel(backgroundColor, 10);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(-5, 5, -5 ,5));
        headerPanel.setLayout(new BorderLayout());
        // add header panel
        JLabel blockType = new JLabel();
        blockType.setForeground(JBColor.foreground());
        headerPanel.add(blockType, BorderLayout.WEST);
        // add button panel
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.setBackground(backgroundColor);
        if (codeType != null && codeType.startsWith(AGENT_TYPE_START)) {
            blockType.setText(codeType.substring(AGENT_TYPE_START.length()));
            // editorTextField.setVisible(false);
            JButton expandButton = createExpandButton(backgroundColor);
            btnPanel.add(expandButton);

            // agent
            codePaneFactory.setAgent(true);
            codePaneFactory.setExpandButton(expandButton);
            expandButton.setIcon(Icons.scaleToWidth(Icons.LOADING_ANIMATED, 16));
        } else {
            blockType.setText("  " + codeType);
            blockType.setFont(blockType.getFont().deriveFont(Font.BOLD));
            int codePos = atomicPos.get() + CODE_IDENTIFIER.length();
            int newLineIndex = content.indexOf('\n', codePos);
            int codeStart = newLineIndex + 1;

            // sql执行联动
            if ("sql".equalsIgnoreCase(codeType)) {
                JButton executeSQLButton = createExecuteSQLButton(codeStart, backgroundColor);
                btnPanel.add(executeSQLButton);
            }

            // 代码块
            JButton diffButton = createDiffButton(codeStart, backgroundColor);
            btnPanel.add(diffButton);
            JButton insertCodeButton = createInsertCodeButton(codeStart, backgroundColor);
            btnPanel.add(insertCodeButton);
            JButton copyButton = createCopyButton(codeStart, backgroundColor);
            btnPanel.add(copyButton);
            if (questionType == QuestionType.UNIT_TEST) {
                JButton newFileButton = createNewFileButton(codeStart, backgroundColor);
                btnPanel.add(newFileButton);
            }
        }
        headerPanel.add(btnPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JButton createExpandButton(Color backgroundColor) {
        JButton expandButton = imgButtonFactory.create("Expand / Hide", Icons.AI_EXPAND, 16, 16
                , JBUI.insets(5), backgroundColor);
        expandButton.setBorderPainted(true);
        expandButton.setContentAreaFilled(true);
        expandButton.addActionListener(e -> {
            editorTextField.setVisible(!editorTextField.isVisible());
        });
        return expandButton;
    }

    private JButton createDiffButton(int codeStart, Color backgroundColor) {
        JButton diffButton = imgButtonFactory.create("比对差异", Icons.AI_DIFF, 13, 13
                , JBUI.insets(5), backgroundColor);
        diffButton.addActionListener(e -> {
            String msg = this.contentRefer.get().substring(codeStart);
            int codeEnd = msg.indexOf(CODE_IDENTIFIER);
            if (codeEnd != -1) {
                msg = msg.substring(0, codeEnd);
            }
            Project project = ApplicationUtil.findCurrentProject();
            Editor editor = EditorUtil.getSelectedEditor(project);
            if (editor == null) {
                NotifyUtils.notify("找不到打开的编辑器", NotificationType.WARNING);
                return;
            }
            Document document = editor.getDocument();
            int start = editor.getSelectionModel().getSelectionStart();
            int end = editor.getSelectionModel().getSelectionEnd();
            String content = document.getText().substring(0, start) + msg + document.getText().substring(end);
            FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension("java");
            content = EditorUtil.formatContent(project, fileType, content, start, start + msg.length());
            EditorUtil.showDiff(editor, content);
        });
        return diffButton;
    }

    private JButton createCopyButton(int codeStart, Color backgroundColor) {
        JButton copyButton = imgButtonFactory.create("复制", Icons.AI_COPY, 13, 13
                , JBUI.insets(5), backgroundColor);
        copyButton.addActionListener(e -> {
            String msg = this.contentRefer.get().substring(codeStart);
            int codeEnd = msg.indexOf(CODE_IDENTIFIER);
            if (codeEnd != -1) {
                msg = msg.substring(0, codeEnd);
            }
            StringSelection selection = new StringSelection(msg);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);
            copyButton.setIcon(Icons.DONE);
            copyButton.setToolTipText("复制成功");
            ChatUtil.saveCodeCompletionLog(ProjectUtils.getCurrProject(), null, null, null
                    , CompletionType.ASK_AI, CompletionStatus.ACCEPT, msg, null);
            executorService.submit(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
                copyButton.setIcon(Icons.AI_COPY);
                copyButton.setToolTipText("复制");
            });
        });
        return copyButton;
    }

    private JButton createInsertCodeButton(int codeStart, Color backgroundColor) {
        JButton insertButton = imgButtonFactory.create("插入代码", Icons.INSERT_CODE, 13, 13
                , JBUI.insets(5), backgroundColor);
        insertButton.addActionListener(e -> {
            String msg = this.contentRefer.get().substring(codeStart);
            int codeEnd = msg.indexOf(CODE_IDENTIFIER);
            if (codeEnd != -1) {
                msg = msg.substring(0, codeEnd);
            }
            CompletionType completionType = CompletionType.ASK_AI;
            if (questionType == QuestionType.COMMENT_CODE) {
                completionType = CompletionType.GENERATE_CODE_COMMENT;
            }
            EditorUtil.replaceTextToSelectedEditor(msg, completionType);
            insertButton.setIcon(Icons.DONE);
            executorService.submit(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
                insertButton.setIcon(Icons.INSERT_CODE);
            });
        });
        return insertButton;
    }

    private JButton createExecuteSQLButton(int codeStart, Color backgroundColor) {
        JButton insertButton = imgButtonFactory.create("执行SQL语句", Icons.ACTION_EXECUTE, 14, 14
                , JBUI.insets(5), backgroundColor);
        insertButton.addActionListener(e -> {
            Project project = ProjectUtils.getCurrProject();
            String sql = this.contentRefer.get().substring(codeStart);
            if (sql.contains(CODE_IDENTIFIER)) sql = sql.substring(0, sql.indexOf(CODE_IDENTIFIER));
            String finalSQL = sql;

            SQLEditorManager.openTempSQLConsole(project, null);
            FileEditorManager editorManager = FileEditorManager.getInstance(project);
            FileEditor fileEditor = editorManager.getSelectedEditor();
            if (Objects.isNull(fileEditor) || !(fileEditor instanceof SQLDataEditor)) return;

            SQLDataEditor selectedEditor = (SQLDataEditor) editorManager.getSelectedEditor();
            Editor editor = selectedEditor.getEditor();
            if (Objects.nonNull(editor)) {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    editor.getDocument().setText(finalSQL);
                });
            }
        });
        return insertButton;
    }

    private JButton createNewFileButton(int codeStart, Color backgroundColor) {
        JButton newFileButton = imgButtonFactory.create("新建文件", Icons.AI_NEW_FILE, 15, 15
                , JBUI.insets(5), backgroundColor);
        newFileButton.addActionListener(e -> {
            try {
                String msg = this.contentRefer.get().substring(codeStart);
                int codeEnd = msg.indexOf(CODE_IDENTIFIER);
                if (codeEnd != -1) {
                    msg = msg.substring(0, codeEnd);
                }
                GenerateTestUtil.createTestFile(msg);
                newFileButton.setIcon(Icons.DONE);
                executorService.submit(() -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {}
                    newFileButton.setIcon(Icons.AI_NEW_FILE);
                });
            } catch (Exception ex) {
                Messages.showMessageDialog(ex.getMessage(), "提示", Messages.getInformationIcon());
                LOG.info("创建文件失败: " + ex.getMessage());
            }
        });
        return newFileButton;
    }

}
