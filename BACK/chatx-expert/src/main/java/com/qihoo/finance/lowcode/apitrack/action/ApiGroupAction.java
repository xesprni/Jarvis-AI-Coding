package com.qihoo.finance.lowcode.apitrack.action;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.apitrack.dialog.ApiDesignDialog;
import com.qihoo.finance.lowcode.apitrack.dialog.ApiGroupDialog;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * ApiAction
 *
 * @author fengjinfu-jk
 * date 2023/9/1
 * @version 1.0.0
 * @apiNote ApiAction
 */
public class ApiGroupAction extends AbstractAction {
    private final boolean isEdit;
    private final Project project;

    public ApiGroupAction(@Nullable String text, Project project, boolean isEdit) {
        super(text);
        this.project = project;
        this.isEdit = isEdit;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new ApiGroupDialog(project, isEdit).showAndGet();
    }
}
