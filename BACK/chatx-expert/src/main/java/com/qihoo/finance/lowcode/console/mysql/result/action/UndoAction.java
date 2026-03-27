package com.qihoo.finance.lowcode.console.mysql.result.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.ui.table.JBTable;
import com.qihoo.finance.lowcode.common.entity.dto.SQLExecuteResult;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.console.mysql.editor.SQLDataEditor;
import com.qihoo.finance.lowcode.console.mysql.result.ui.ResultView;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import org.jetbrains.annotations.NotNull;

/**
 * ApplyModifyAction
 *
 * @author fengjinfu-jk
 * date 2024/2/4
 * @version 1.0.0
 * @apiNote ApplyModifyAction
 */
public class UndoAction extends AnAction implements DumbAware {
    private final JBTable table;
    private final ResultView.TablePageBtnPanel pageBtnPanel;
    private final ResultView form;

    public UndoAction(JBTable table, ResultView.TablePageBtnPanel pageBtnPanel) {
        super("放弃更改", "放弃更改", Icons.scaleToWidth(Icons.ROLLBACK2, 16));
        this.table = table;
        this.pageBtnPanel = pageBtnPanel;
        this.form = ProjectUtils.getCurrProject().getService(ResultView.class);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        pageBtnPanel.getReloadAction().actionPerformed(null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        SQLExecuteResult data = (SQLExecuteResult) pageBtnPanel.getTableScroll().getClientProperty(SQLDataEditor.EXECUTE_RESULT);
        e.getPresentation().setEnabled(form.hadModified(table, data));
    }
}
