package com.qihoo.finance.lowcode.gentracker.service;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import com.qihoo.finance.lowcode.gentracker.dto.TableInfoSettingsDTO;
import com.qihoo.finance.lowcode.gentracker.entity.TableInfo;
import com.qihoo.finance.lowcode.gentracker.service.impl.TableInfoSettingsServiceImpl;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;

import java.io.IOException;

/**
 * 表设置
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public interface TableInfoSettingsService extends PersistentStateComponent<TableInfoSettingsDTO> {
    /**
     * 获取实例
     *
     * @return {@link SettingsStorageService}
     */
    static TableInfoSettingsService getInstance() {
        try {
            return ServiceManager.getService(ProjectUtils.getCurrProject(), TableInfoSettingsService.class);
        } catch (AssertionError e) {
            // 出现配置文件被错误修改，或不兼容时直接删除配置文件。
            VirtualFile workspaceFile = ProjectUtils.getCurrProject().getWorkspaceFile();
            if (workspaceFile != null) {
                VirtualFile configFile = workspaceFile.getParent().findChild("tableSetting.xml");
                if (configFile != null && configFile.exists()) {
                    WriteCommandAction.runWriteCommandAction(ProjectUtils.getCurrProject(), () -> {
                        try {
                            configFile.delete(null);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            }
            // 重新获取配置
            return ServiceManager.getService(ProjectUtils.getCurrProject(), TableInfoSettingsServiceImpl.class);
        }
    }

    /**
     * 获取表格信息
     *
     * @param dbTable 数据库表
     * @return {@link TableInfo}
     */
    TableInfo getTableInfo(MySQLTableNode dbTable);

    TableInfo getMemoryTableInfo(MySQLTableNode dbTable);

    /**
     * 保存表信息
     *
     * @param tableInfo 表信息
     */
    void saveTableInfo(TableInfo tableInfo);

    /**
     * 重置表信息
     *
     * @param dbTable 数据库表
     */
    void resetTableInfo(MySQLTableNode dbTable);

    /**
     * 删除表信息
     *
     * @param dbTable 数据库表
     */
    void removeTableInfo(MySQLTableNode dbTable);
}
