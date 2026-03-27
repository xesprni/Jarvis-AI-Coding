package com.qihoo.finance.lowcode.console.mysql.execute.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.qihoo.finance.lowcode.common.ui.base.RoundedLabel;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class SQLEditorToolbar implements Disposable {
    private final JPanel actionsPanel;

    public SQLEditorToolbar(Project project, FileEditor fileEditor) {
        actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        ActionManager actionManager = ActionManager.getInstance();
        DefaultActionGroup actionGroup = (DefaultActionGroup) actionManager.getAction("ChatX.ActionGroup.SQLFileEditor");
        ActionToolbar actionToolbar = actionManager.createActionToolbar("bar", actionGroup, true);
        actionToolbar.setTargetComponent(actionsPanel);
        actionToolbar.setOrientation(SwingConstants.HORIZONTAL);

        JLabel sqlLabel = new JLabel(GlobalDict.PLUGIN_NAME);
        sqlLabel.setIcon(Icons.scaleToWidth(Icons.LOGO_ROUND, 16));
        actionsPanel.add(sqlLabel);

        // actionToolbar
        actionsPanel.add(actionToolbar.getComponent());

        JLabel tipsLabel = new JLabel("Ctrl + S 保存      Shift + F6 重命名      Ctrl + Enter 支持选中执行");
        tipsLabel.setForeground(RoundedLabel.GREEN);
        actionsPanel.add(tipsLabel);
        actionsPanel.setBorder(BorderFactory.createEmptyBorder(-2, 0, -2, 0));
    }

    @NotNull
    public JPanel getMainComponent() {
        return this.actionsPanel;
    }

    @Override
    public void dispose() {
        Disposer.dispose(this);
    }
}
