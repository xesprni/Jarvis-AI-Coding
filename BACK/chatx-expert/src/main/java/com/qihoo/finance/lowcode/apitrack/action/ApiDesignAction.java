package com.qihoo.finance.lowcode.apitrack.action;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.apitrack.dialog.ApiDesignDialog;
import com.qihoo.finance.lowcode.common.constants.OperateType;
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
public class ApiDesignAction extends AbstractAction {
    private final OperateType operateType;
    private final Project project;

    public ApiDesignAction(@Nullable String text, Project project, OperateType operateType) {
        super(text);
        this.project = project;
        this.operateType = operateType;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new ApiDesignDialog(project, operateType).show();
    }
}
