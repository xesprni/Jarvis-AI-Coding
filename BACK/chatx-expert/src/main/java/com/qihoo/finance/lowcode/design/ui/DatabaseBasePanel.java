package com.qihoo.finance.lowcode.design.ui;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.common.ui.base.BasePanel;
import com.qihoo.finance.lowcode.design.constant.DatabaseOperateType;
import org.jetbrains.annotations.NotNull;

import java.awt.Component;

/**
 * @author weiyichao
 * @date 2023-07-31
 **/
public abstract class DatabaseBasePanel extends BasePanel {
    private DatabaseOperateType operateType;

    public DatabaseBasePanel(@NotNull Project project) {
        super(project);
    }

    /**
     * 实例化面板
     */
    public abstract Component createPanel();

    public DatabaseOperateType getOperateType() {
        return operateType;
    }

    public void setOperateType(DatabaseOperateType operateType) {
        this.operateType = operateType;
    }

    public boolean isEdit() {
        return DatabaseOperateType.EDIT == operateType;
    }

    public boolean isCopy(){
        return DatabaseOperateType.COPY == operateType;
    }

    public boolean isCreate(){
        return DatabaseOperateType.CREATE == operateType;
    }
}
