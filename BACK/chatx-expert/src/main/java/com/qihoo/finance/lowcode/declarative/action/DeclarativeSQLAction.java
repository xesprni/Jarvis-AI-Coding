package com.qihoo.finance.lowcode.declarative.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.declarative.ui.DeclarativeDialog;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * DeclarativeSQLChangeAction
 *
 * @author fengjinfu-jk
 * date 2024/4/23
 * @version 1.0.0
 * @apiNote DeclarativeSQLChangeAction
 */
public class DeclarativeSQLAction extends AnAction {
    @Setter
    @Getter
    private String declarativeSQLFilePath = "-1";
    public static final String ACTION_ID = "Chatx.declarative";

    public DeclarativeSQLAction() {
        super("声明式SQL变更", "MySQL声明式变更", Icons.scaleToWidth(Icons.FTD, 16));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (Objects.isNull(project)) return;

        VirtualFile[] files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
        if (Objects.isNull(files)) return;

        Set<VirtualFile> selectFiles = new HashSet<>();
        for (VirtualFile file : files) {
            file.refresh(true, true);
            if (file.isDirectory()) {
                VirtualFile[] children = file.getChildren();
                for (VirtualFile child : children) {
                    if (child.getName().endsWith(DeclarativeDialog.SQL_SUFFIX)) {
                        selectFiles.add(child);
                    }
                }
            } else {
                selectFiles.add(file);
            }
        }

        new DeclarativeDialog(project, selectFiles, declarativeSQLFilePath).show();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        // file
        VirtualFile[] files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
        files = files == null ? new VirtualFile[]{} : files;
        if (files.length == 0) {
            presentation.setVisible(false);
            super.update(e);
            return;
        }

        for (VirtualFile file : files) {
            if (!isDeclarativeSQLFile(file)) {
                presentation.setVisible(false);
                super.update(e);
                return;
            }
            file.refresh(true, true);
        }

        // database
        List<DatabaseNode> databaseNodes = DataContext.getInstance(ProjectUtils.getCurrProject()).getAllMySQLDatabaseList();
        presentation.setVisible(CollectionUtils.isNotEmpty(databaseNodes));
        super.update(e);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    private boolean isDeclarativeSQLFile(VirtualFile file) {
        if (Objects.isNull(file)) return false;

        String path = file.getPath();
        if (file.isDirectory() && path.contains(declarativeSQLFilePath)) {
            VirtualFile[] children = file.getChildren();
            for (VirtualFile child : children) {
                if (child.getName().endsWith(DeclarativeDialog.SQL_SUFFIX)) {
                    return true;
                }
            }
            // 文件夹下没有符合的sql文件
            return false;
        }
        return path.contains(declarativeSQLFilePath) && path.endsWith(DeclarativeDialog.SQL_SUFFIX);
    }
}
