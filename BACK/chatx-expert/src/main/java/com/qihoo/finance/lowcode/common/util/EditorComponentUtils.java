package com.qihoo.finance.lowcode.common.util;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;
import com.qihoo.finance.lowcode.common.enums.LightVirtualType;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.gentracker.ui.base.EditorSettingsInit;

import java.awt.*;

/**
 * CusEditorComponent
 *
 * @author fengjinfu-jk
 * date 2023/10/11
 * @version 1.0.0
 * @apiNote CusEditorComponent
 */
public class EditorComponentUtils {
    public static final Color BACKGROUND;

    static {
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument("");
        document.setReadOnly(false);
        EditorImpl editor = (EditorImpl) editorFactory.createEditor(document);

        BACKGROUND = editor.getBackgroundColor();
    }

    public static Editor createEditorPanel(Project project, LightVirtualType virtualType) {
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument("");
        document.setReadOnly(false);
        EditorImpl editor = (EditorImpl) editorFactory.createEditor(document);
        // 初始默认设置
        EditorSettingsInit.init(editor);
        // 添加监控事件
        editor.getDocument().setReadOnly(false);

        EditorHighlighterFactory highlighterFactory = EditorHighlighterFactory.getInstance();
        editor.setHighlighter(highlighterFactory.createEditorHighlighter(project, new LightVirtualFile(virtualType.getValue())));

        return editor;
    }

    public static void setHighlighter(Editor editor, LightVirtualType virtualType) {
        if (editor instanceof EditorImpl editorImpl) {
            EditorHighlighterFactory highlighterFactory = EditorHighlighterFactory.getInstance();
            editorImpl.setHighlighter(highlighterFactory.createEditorHighlighter(ProjectUtils.getCurrProject()
                    , new LightVirtualFile(virtualType.getValue())));
        }
    }

    public static Editor createEditorPanel() {
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument("");
        document.setReadOnly(false);
        EditorImpl editor = (EditorImpl) editorFactory.createEditor(document);
        // 初始默认设置
        EditorSettingsInit.initFalse(editor);
        // 添加监控事件
        editor.getDocument().setReadOnly(false);

        return editor;
    }

    public static void write(Project project, Editor editor, String text, boolean readOnly) {
        WriteCommandAction.runWriteCommandAction(project, () -> {
            editor.getDocument().setReadOnly(false);
            editor.getDocument().setText(text);
            editor.getDocument().setReadOnly(readOnly);
        });
    }

    public static void write(Project project, Editor editor, String text) {
        write(project, editor, text, true);
    }

    public static void write(Editor editor, String text) {
        write(ProjectUtils.getCurrProject(), editor, text, true);
    }

    public static void write(Editor editor, String text, boolean readOnly) {
        write(ProjectUtils.getCurrProject(), editor, text, readOnly);
    }

    private static final String JAVA_SUFFIX = ".java";
    private static final String XML_SUFFIX = ".xml";
    private static final String FTL_SUFFIX = ".ftl";
    private static final String JSON_SUFFIX = ".json";
    private static final String VM_SUFFIX = ".java.vm";
    private static final String MARKDOWN_SUFFIX = ".md";
}
