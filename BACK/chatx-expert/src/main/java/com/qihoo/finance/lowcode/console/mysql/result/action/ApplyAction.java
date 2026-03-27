package com.qihoo.finance.lowcode.console.mysql.result.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.table.JBTable;
import com.qihoo.finance.lowcode.common.entity.dto.SQLExecuteResult;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.console.mysql.editor.SQLDataEditor;
import com.qihoo.finance.lowcode.console.mysql.result.ui.ResultView;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;
import java.util.List;

/**
 * ApplyModifyAction
 *
 * @author fengjinfu-jk
 * date 2024/2/4
 * @version 1.0.0
 * @apiNote ApplyModifyAction
 */
public class ApplyAction extends AnAction implements DumbAware {
    private final DatabaseNode database;
    private final JBTable table;
    private final ResultView.TablePageBtnPanel pageBtnPanel;
    private final ResultView resultView;

    public ApplyAction(DatabaseNode database, JBTable table, ResultView.TablePageBtnPanel pageBtnPanel) {
        super("应用更改", "应用更改", Icons.scaleToWidth(Icons.SELECTED, 16));
        this.database = database;
        this.table = table;
        this.pageBtnPanel = pageBtnPanel;
        this.resultView = ProjectUtils.getCurrProject().getService(ResultView.class);

        if (SystemInfo.isMac) {
            registerCustomShortcutSet(KeyEvent.VK_S, KeyEvent.META_DOWN_MASK, table);
        } else {
            registerCustomShortcutSet(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, table);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        int update = Messages.showDialog("确定要应用更改?", "应用更改", new String[]{"是", "否"}, Messages.NO, Icons.scaleToWidth(Icons.WARN, 50));
        if (update == Messages.YES) {
            SQLExecuteResult newData = (SQLExecuteResult) pageBtnPanel.getTableScroll().getClientProperty(SQLDataEditor.EXECUTE_RESULT);
            List<String> sqlList = resultView.executeUpdate(table, newData);
            String sql = String.join("\n", sqlList);
            resultView.executeSQLAndShowResult(database, pageBtnPanel, sql);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        SQLExecuteResult data = (SQLExecuteResult) pageBtnPanel.getTableScroll().getClientProperty(SQLDataEditor.EXECUTE_RESULT);
        e.getPresentation().setEnabled(resultView.hadModified(table, data));
    }
}
