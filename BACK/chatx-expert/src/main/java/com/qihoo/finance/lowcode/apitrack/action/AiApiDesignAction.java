package com.qihoo.finance.lowcode.apitrack.action;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.apitrack.dialog.AiApiDesignDialog;
import com.qihoo.finance.lowcode.apitrack.dialog.ApiDesignDialog;
import com.qihoo.finance.lowcode.apitrack.entity.ai.AiApiNode;
import com.qihoo.finance.lowcode.common.constants.OperateType;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

/**
 * ApiAction
 *
 * @author fengjinfu-jk
 * date 2023/9/1
 * @version 1.0.0
 * @apiNote ApiAction
 */
public class AiApiDesignAction extends AbstractAction {
    private final OperateType operateType;
    private final Project project;
    private final AiApiNode apiNode;

    public AiApiDesignAction(@Nullable String text, Project project, OperateType operateType, AiApiNode apiNode) {
        super(text);
        this.project = project;
        this.operateType = operateType;
        this.apiNode = apiNode;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new AiApiDesignDialog(project, operateType, apiNode).show();
    }
}
