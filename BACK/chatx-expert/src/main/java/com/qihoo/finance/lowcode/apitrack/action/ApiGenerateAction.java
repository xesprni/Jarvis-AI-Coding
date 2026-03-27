package com.qihoo.finance.lowcode.apitrack.action;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.apitrack.dialog.ApiDesignDialog;
import com.qihoo.finance.lowcode.apitrack.dialog.ApiGenCodeDialog;
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
public class ApiGenerateAction extends AbstractAction {
    private final Project project;

    public ApiGenerateAction(@Nullable String text, Project project) {
        super(text);
        this.project = project;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new ApiGenCodeDialog(project).show();
    }
}
