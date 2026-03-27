package com.qihoo.finance.lowcode.common.ui.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SimpleDialog
 *
 * @author fengjinfu-jk
 * date 2024/2/1
 * @version 1.0.0
 * @apiNote SimpleDialog
 */
public class SimpleDialog extends DialogWrapper {
    private final JComponent centerPanel;
    private final List<Runnable> doOKActions;

    public SimpleDialog(@NotNull Project project, String title, String okBtnTxt, String cancelBtnTxt, JComponent centerPanel) {
        super(project);
        this.doOKActions = new ArrayList<>();
        this.centerPanel = centerPanel;

        init();
        setTitle(title);
        setOKButtonText(okBtnTxt);
        setCancelButtonText(cancelBtnTxt);
    }

    public SimpleDialog(@NotNull Project project, String title, String okBtnTxt, String cancelBtnTxt, JComponent centerPanel, Runnable... doOKAction) {
        this(project, title, okBtnTxt, cancelBtnTxt, centerPanel);
        this.doOKActions.addAll(Arrays.stream(doOKAction).collect(Collectors.toList()));
    }

    @Override
    protected void doOKAction() {
        for (Runnable doOKAction : this.doOKActions) {
            doOKAction.run();
        }
        super.doOKAction();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return centerPanel;
    }
}
