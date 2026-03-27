package com.qihoo.finance.lowcode.aiquestion.ui.component;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.ide.highlighter.custom.CustomFileHighlighter;
import com.intellij.ide.highlighter.custom.SyntaxTable;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.util.textCompletion.TextCompletionProvider;
import com.intellij.util.textCompletion.TextFieldWithCompletion;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.aiquestion.ui.QuestionPanel;
import com.qihoo.finance.lowcode.aiquestion.ui.factory.InputPanelFactory;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.dto.askai.AssistantInfo;
import com.qihoo.finance.lowcode.common.entity.dto.askai.ShortcutInstructionInfo;
import com.qihoo.finance.lowcode.common.entity.dto.plugins.DatasetInfo;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.qihoo.finance.lowcode.aiquestion.ui.factory.InputPanelFactory.FC_TX;

public class InputEditorTextField extends TextFieldWithCompletion {
    public static final Key<Boolean> ASK_AI_EDITOR = Key.create("chatx.ask.input.editor");

    private final Color background;
    private final DumbAwareAction enterAction;

    public static final String CLEAR_INSTRUCTION = "clear conversation";

    @Override
    public @Nullable Editor getEditor() {
        return super.getEditor();
    }

    public InputEditorTextField(@NotNull Project project, @NotNull TextCompletionProvider provider, @NotNull String value
            , boolean oneLineMode, boolean forceAutoPopup, boolean showHint, Color background, DumbAwareAction enterAction) {
        super(project, provider, value, oneLineMode, forceAutoPopup, forceAutoPopup, showHint, false);
        this.enterAction = enterAction;
        this.background = background;
    }

    @Override
    protected @NotNull EditorEx createEditor() {
        EditorEx editor = super.createEditor();
        // 设置换行位置为根据缩进级别
        editor.getSettings().setUseSoftWraps(true);
        editor.getSettings().setUseCustomSoftWrapIndent(false);

        editor.setShowPlaceholderWhenFocused(true);
        editor.setEmbeddedIntoDialogWrapper(true);

        setHighlighter(editor);
        editor.setBorder(null);
//        UIUtil.setNotOpaqueRecursively(editor.getComponent());
        editor.putUserData(ASK_AI_EDITOR, true);
        editor.putUserData(AutoPopupController.NO_ADS, true);
        DumbAwareAction newLineAction = new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                insertNewLine();
            }
        };
        newLineAction.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, editor.getComponent());
        if (enterAction != null) {
            enterAction.registerCustomShortcutSet(CommonShortcuts.ENTER, editor.getContentComponent());
        }
        Disposable inputDisposable = Disposer.newDisposable("inputListener");
        EditorUtil.disposeWithEditor(editor, inputDisposable);
        editor.getDocument().addDocumentListener(new InputDocumentListener(editor), inputDisposable);
        return editor;
    }

    private void setHighlighter(EditorEx editor) {
        SyntaxTable table = new SyntaxTable();

        for (ShortcutInstructionInfo instructionInfo : ChatxApplicationSettings.settings().shortcutInstructions) {
            table.addKeyword2("/" + instructionInfo.getName());
        }

        table.addKeyword3("#ALL");
        for (DatasetInfo dataset : ChatxApplicationSettings.settings().datasets) {
            table.addKeyword3("#" + dataset.getDatasetName());
        }

        for (AssistantInfo assistantInfo : ChatxApplicationSettings.settings().assistants) {
            table.addKeyword4("@" + assistantInfo.getName());
        }
        editor.setHighlighter(
                HighlighterFactory.createHighlighter(new CustomFileHighlighter(table), editor.getColorsScheme())
        );
    }

    @RequiredArgsConstructor
    static class InputDocumentListener implements DocumentListener {
        @NotNull
        private final EditorEx editor;
        private final static ExecutorService executorService = Executors.newFixedThreadPool(1);

        private static void waiting(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        @Override
        public void beforeDocumentChange(@NotNull DocumentEvent event) {
            DocumentListener.super.beforeDocumentChange(event);
        }

        @Override
        public void documentChanged(@NotNull DocumentEvent event) {
            Document document = event.getDocument();
            String text = document.getText();
            String key = text.substring(event.getOffset(), Math.min(text.length(), event.getOffset() + 1));
            if (key.equals("/") || key.equals("#") || key.equals("@")) {
                AutoPopupController.getInstance(Objects.requireNonNull(editor.getProject())).scheduleAutoPopup(editor);
            }

            // 切换助手
            selectAssistant(document);
            // 命令行
            selectInstructions(document);
            // 知识库
            selectDataset(document);
            // 清空
            if (StringUtils.isEmpty(text)) {
                clearCustomElementRenderer(editor);
            }
        }

        private void selectDataset(Document document) {
            final String text = document.getText();
            boolean nothingDataset = text.contains("#_NOTHING_DATASET");
            if (!nothingDataset) return;

            // 替换
            String replaceText = StringUtils.substringBefore(text, "#_NOTHING_DATASET")
                    + StringUtils.substringAfter(text, "#_NOTHING_DATASET");
            executorService.submit(() -> {
                waiting(10);
                UIUtil.invokeLaterIfNeeded(() -> {
                    WriteCommandAction.writeCommandAction(ProjectUtils.getCurrProject()).run(() -> {
                        try {
                            document.setText(replaceText);
                        } catch (Exception e) {
                            // 再给一次机会
                            waiting(50);
                            document.setText(replaceText);
                        }
                    });
                });
            });
        }

        private void selectInstructions(Document document) {
            final String text = document.getText();
            ShortcutInstructionInfo instruction = ChatxApplicationSettings.settings().shortcutInstructions.stream()
                    .filter(i -> text.contains("/" + i.getName())).findFirst().orElse(new ShortcutInstructionInfo());
            if (StringUtils.isEmpty(instruction.getName())) return;

            QuestionPanel questionPanel = ProjectUtils.getCurrProject().getService(QuestionPanel.class);
            // 清空
            boolean clear = instruction.getName().equals(CLEAR_INSTRUCTION);
            if (instruction.getName().equals(CLEAR_INSTRUCTION)) {
                questionPanel.repaintPanel();
            }
            // 快捷指令
            InputPanelFactory inputPanel = questionPanel.getInputPanelFactory();
            String replaceText = inputPanel.packagePrompt(instruction, text);
            // 替换
            executorService.submit(() -> {
                waiting(10);
                UIUtil.invokeLaterIfNeeded(() -> {
                    WriteCommandAction.writeCommandAction(ProjectUtils.getCurrProject()).run(() -> {
                        try {
                            document.setText(replaceText);
                        } catch (Exception e) {
                            // 再给一次机会
                            waiting(50);
                            document.setText(replaceText);
                        }
                    });
                    InputPanelFactory inputPanelFactory = questionPanel.getInputPanelFactory();
                    if (clear) {
                        inputPanelFactory.getFastCommand().setText(FC_TX);
                        inputPanelFactory.getFastCommand().setIcon(Icons.scaleToWidth(inputPanelFactory.getDefaultCommandIcon(), 13));
                        inputPanelFactory.flushAssistant(Constants.DEFAULT_ASSISTANT);
                    } else {
                        inputPanelFactory.flushAssistant(instruction.getAssistantCode());
                    }
                });
            });
        }

        private void selectAssistant(Document document) {
            String text = document.getText();
            AssistantInfo assistant = ChatxApplicationSettings.settings().assistants.stream()
                    .filter(a -> text.equals("@" + a.getName())).findFirst().orElse(new AssistantInfo());
            boolean clear = text.contains("@不使用助手");
            assistant.setCode(clear ? "CLEAR ASSISTANT" : assistant.getCode());
            assistant.setName(clear ? "不使用助手" : assistant.getName());

            if (StringUtils.isNotEmpty(assistant.getCode())) {
                QuestionPanel questionPanel = ProjectUtils.getCurrProject().getService(QuestionPanel.class);
                if (questionPanel == null) return;
                // 切换助手
                String replaceText = StringUtils.substringBefore(text, "@" + assistant.getName())
                        + StringUtils.substringAfter(text, "@" + assistant.getName());
                executorService.submit(() -> {
                    waiting(10);
                    UIUtil.invokeLaterIfNeeded(() -> {
                        WriteCommandAction.writeCommandAction(ProjectUtils.getCurrProject()).run(() -> {
                            try {
                                document.setText(replaceText);
                            } catch (Exception e) {
                                // 再给一次机会
                                waiting(50);
                                document.setText(replaceText);
                            }
                        });
                        InputPanelFactory inputPanelFactory = questionPanel.getInputPanelFactory();
                        if (clear) {
                            inputPanelFactory.getFastCommand().setText(FC_TX);
                            inputPanelFactory.getFastCommand().setIcon(Icons.scaleToWidth(inputPanelFactory.getDefaultCommandIcon(), 13));
                            inputPanelFactory.flushAssistant(Constants.DEFAULT_ASSISTANT);
                        } else {
                            inputPanelFactory.flushAssistant(assistant.getCode());
                        }
                        inputPanelFactory.repaintNorthContent();
                    });
                });
            }
        }
    }

    public static void clearCustomElementRenderer(Editor editor) {
        Document document = editor.getDocument();
        int endOffset = document.getLineEndOffset(Math.max(document.getLineCount() - 1, 0));
        InlayModel inlayModel = editor.getInlayModel();
        List<Inlay<? extends EditorCustomElementRenderer>> inlays =
                inlayModel.getInlineElementsInRange(-1, endOffset, EditorCustomElementRenderer.class);
        inlays.forEach(Disposable::dispose);
    }

    private void insertNewLine() {
        Editor editor = getEditor();
        WriteCommandAction.writeCommandAction(getProject()).run(() -> {
            int offset = editor.getSelectionModel().getSelectionEnd();
            editor.getDocument().insertString(offset, "\n");
            editor.getCaretModel().moveToOffset(offset + 1);
        });
    }
}
