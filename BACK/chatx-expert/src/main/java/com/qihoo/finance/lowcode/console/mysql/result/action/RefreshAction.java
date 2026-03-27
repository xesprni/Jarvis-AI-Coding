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

/**
 * ApplyModifyAction
 *
 * @author fengjinfu-jk
 * date 2024/2/4
 * @version 1.0.0
 * @apiNote ApplyModifyAction
 */
public class RefreshAction extends AnAction implements DumbAware {
    private final DatabaseNode database;
    private final JBTable table;
    private final ResultView.TablePageBtnPanel pageBtnPanel;
    private final ResultView resultView;

    public RefreshAction(DatabaseNode database, JBTable table, ResultView.TablePageBtnPanel pageBtnPanel) {
        super("刷新", "刷新", Icons.scaleToWidth(Icons.RELOAD, 16));
        this.database = database;
        this.table = table;
        this.pageBtnPanel = pageBtnPanel;
        this.resultView = ProjectUtils.getCurrProject().getService(ResultView.class);

        if (SystemInfo.isMac) {
            registerCustomShortcutSet(KeyEvent.VK_F5, 0, table);
        } else {
            registerCustomShortcutSet(KeyEvent.VK_F5, 0, table);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        SQLExecuteResult data = (SQLExecuteResult) pageBtnPanel.getTableScroll().getClientProperty(SQLDataEditor.EXECUTE_RESULT);
        boolean hadModified = resultView.hadModified(table, data);
        if (hadModified) {
            int update = Messages.showDialog("表数据已被修改，你确定要刷新它而不应用更改吗?", "刷新", new String[]{"是", "否"}, Messages.NO, Icons.scaleToWidth(Icons.WARN, 50));
            if (update == Messages.NO) return;
        }

        pageBtnPanel.getReloadAction().actionPerformed(null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }
}
