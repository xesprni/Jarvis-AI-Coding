package com.qihoo.finance.lowcode.console.mysql.execute.listener;

import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.qihoo.finance.lowcode.common.configuration.UserContextPersistent;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.dto.SQLExecuteHistory;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.console.mysql.SQLEditorManager;
import com.qihoo.finance.lowcode.console.mysql.editor.SQLDataEditor;
import com.qihoo.finance.lowcode.console.mysql.execute.ui.SQLEditorToolbar;
import com.qihoo.finance.lowcode.console.mysql.execute.ui.dialog.SaveSQLDialog;
import com.qihoo.finance.lowcode.console.mysql.result.ui.ResultView;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

/**
 * SQLConsoleEditorListener
 *
 * @author fengjinfu-jk
 * date 2023/9/12
 * @version 1.0.0
 * @apiNote SQLConsoleEditorListener
 */
@Slf4j
public class SQLEditorFileListener implements FileEditorManagerListener, ProjectManagerListener, ApplicationListener {

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (notSqlConsoleFile(source, file)) return;

        log.info("fileOpened: {}", file.getPath());
        Project project = source.getProject();
        FileEditor fileEditor = source.getSelectedEditor(file);

        String datasource = file.getUserData(SQLEditorManager.SQL_CONSOLE_DATASOURCE);
        if (StringUtils.isEmpty(datasource)) {
            datasource = StringUtils.defaultIfEmpty(DataContext.getInstance(project).getDatasourceType(), Constants.DataSource.MySQL);
        }

        file.putUserData(SQLEditorManager.SQL_CONSOLE_DATASOURCE, datasource);
        if (Constants.DataSource.MySQL.equals(datasource)) {
            mysqlSQLConsole(source, file, fileEditor, project);
            log.info(Constants.Log.USER_ACTION, "用户打开MySQL SQL执行");
        }
    }

    private void mysqlSQLConsole(@NotNull FileEditorManager source, @NotNull VirtualFile file, FileEditor fileEditor, Project project) {
        if (Objects.isNull(fileEditor)) return;
        if (!(fileEditor instanceof SQLDataEditor mySQLEditor)) return;
        file.putUserData(SQLEditorManager.SQL_DATA_EDITOR, mySQLEditor);

        // 追加顶部操作工具栏
        SQLEditorToolbar toolbarForm = new SQLEditorToolbar(project, fileEditor);
        source.addTopComponent(fileEditor, toolbarForm.getMainComponent());
        // 清空文本, 注意: ide部分高子版本(例如2023.1.5)的FileLister触发时机会滞后, 导致Document被赋值后, 又被这里清空了
        // clearEditor(source, mySQLEditor.getEditor());
        // 快捷键
        settingKeymap(source, mySQLEditor.getEditor());
        // 记录打开
        SQLEditorManager.consoles.put(file.getName(), file);
    }

    private void clearEditor(FileEditorManager source, Editor editor) {
        if (Objects.nonNull(editor)) {
            ApplicationManager.getApplication().runWriteAction(() -> {
                // runWriteAction保证操作原子性
                boolean writable = editor.getDocument().isWritable();
                editor.getDocument().setReadOnly(false);
                WriteCommandAction.runWriteCommandAction(source.getProject(), () -> editor.getDocument().setText(StringUtils.EMPTY));
                editor.getDocument().setReadOnly(!writable);
            });
        }
    }

    private void settingHighLight(FileEditorManager editorManager, FileType fileType) {
        Editor editor = editorManager.getSelectedTextEditor();
        if (Objects.nonNull(editor) && editor instanceof EditorEx) {
            ((EditorEx) editor).setHighlighter(HighlighterFactory.createHighlighter(editor.getProject(), fileType));
        }
    }

    private void settingKeymap(@NotNull FileEditorManager editorManager, Editor editor) {
        // shortcut
        if (Objects.isNull(editor)) return;
        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                ActionManager.getInstance().getAction("ChatX.Actions.SQLExecute").actionPerformed(anActionEvent);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        }.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, editor.getComponent());

        CustomShortcutSet ctrlS = new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                SystemInfo.isMac ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK));
        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                // 保存
                ActionManager.getInstance().getAction("ChatX.Actions.SaveSQL").actionPerformed(anActionEvent);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        }.registerCustomShortcutSet(ctrlS, editor.getComponent());

        CustomShortcutSet ctrlR = new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_F6, InputEvent.SHIFT_DOWN_MASK));
        new DumbAwareAction() {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                // 保存
                ActionManager.getInstance().getAction("ChatX.Actions.RenameSQL").actionPerformed(anActionEvent);
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        }.registerCustomShortcutSet(ctrlR, editor.getComponent());
    }

    private boolean needClose(@NotNull Project project, VirtualFile file) {
        if (MapUtils.isEmpty(DataContext.getInstance(project).getConsoleDatabase())) {
            String fileName = file.getName();
            SQLEditorManager.consoles.remove(fileName);
            ResultView resultView = ProjectUtils.getCurrProject().getService(ResultView.class);
            resultView.removeTab(fileName);

            // 关闭虚拟文件
            ApplicationManager.getApplication().invokeLater(() -> {
                FileEditorManager.getInstance(project).closeFile(file);
            });
            return true;
        }

        return false;
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        if (isNotValid(file)) return;
        if (!isDbConsoleFile(file)) return;

        Project project = source.getProject();
        // 检测是否需要提示文档内容被修改而未保存
        SQLExecuteHistory history = file.getUserData(SQLEditorManager.SQL_HISTORY);
        SQLDataEditor dataEditor = file.getUserData(SQLEditorManager.SQL_DATA_EDITOR);
        if (Objects.nonNull(history) && Objects.nonNull(dataEditor)) {
            String sqlContent = history.getSqlContent();
            String currentSql = dataEditor.getEditor().getDocument().getText();
            if (!sqlContent.equals(currentSql)) {
                // 文档内容发生变更, 提示保存
                DatabaseNode queryDatabase = DataContext.getInstance(project).getConsoleDatabase(file.getName());
                new SaveSQLDialog(project, file, queryDatabase, currentSql, history).showBeforeClose();
            }
        }

        SQLEditorManager.consoles.remove(file.getName());
        ResultView resultView = project.getService(ResultView.class);
        resultView.removeTab(file.getName());
        DataContext.getInstance(project).removeConsoleDatabase(file.getName());

        FileEditorManagerListener.super.fileClosed(source, file);
        log.info(Constants.Log.USER_ACTION, "用户关闭SQL执行窗口");
    }

    private static boolean isNotValid(Object object) {
        return !isValid(object);
    }

    private static boolean isValid(Object object) {
        if (object == null) {
            return false;
        }

        if (object instanceof Project) {
            Project project = (Project) object;
            return !project.isDisposed();
        }

        if (object instanceof Editor) {
            Editor editor = (Editor) object;
            return !editor.isDisposed();
        }

        if (object instanceof FileEditor) {
            FileEditor editor = (FileEditor) object;
            return editor.isValid();
        }

        if (object instanceof VirtualFile) {
            VirtualFile virtualFile = (VirtualFile) object;
            return virtualFile.isValid();
        }

        if (object instanceof PsiFile) {
            PsiFile psiFile = (PsiFile) object;
            return psiFile.isValid();
        }

        return true;
    }


    private static boolean isDbConsoleFile(VirtualFile file) {
        return SQLEditorManager.isSQLConsole(file);
    }

    private boolean notSqlConsoleFile(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        return !(isValid(file) && isDbConsoleFile(file)) || needClose(source.getProject(), file);
    }
}
