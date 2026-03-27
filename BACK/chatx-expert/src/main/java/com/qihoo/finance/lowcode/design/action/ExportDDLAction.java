package com.qihoo.finance.lowcode.design.action;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.design.dialog.DatabaseExportDDL;
import com.qihoo.finance.lowcode.design.dto.ExportDDLDTO;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * MySQLDeclarativeChange
 *
 * @author fengjinfu-jk
 * date 2023/12/28
 * @version 1.0.0
 * @apiNote MySQLDeclarativeChange
 */
public class ExportDDLAction extends AbstractAction {
    public ExportDDLAction(String name) {
        super(name);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Project project = ProjectUtils.getCurrProject();
        JTree tree = DataContext.getInstance(project).getDbTree();
        TreePath[] selectionPaths = tree.getSelectionPaths();
        if (Objects.isNull(selectionPaths)) return;

        List<ExportDDLDTO> ddlList = new ArrayList<>();
        for (TreePath selectionPath : selectionPaths) {
            Object node = selectionPath.getLastPathComponent();
            if (node instanceof DatabaseNode) {
                tree.setSelectionPath(selectionPath);
                // 库级别导出
                DatabaseNode databaseNode = (DatabaseNode) node;
                if (databaseNode.getChildCount() > 0) {
                    for (Enumeration<? extends TreeNode> children = databaseNode.children(); children.hasMoreElements(); ) {
                        TreeNode treeNode = children.nextElement();
                        if (treeNode instanceof MySQLTableNode) {
                            MySQLTableNode tableNode = (MySQLTableNode) treeNode;
                            ddlList.add(ExportDDLDTO.builder().database(databaseNode).table(tableNode).build());
                        }
                    }
                }
            }
            if (node instanceof MySQLTableNode) {
                // 按表导出
                MySQLTableNode tableNode = (MySQLTableNode) node;
                DatabaseNode databaseNode = (DatabaseNode) tableNode.getParent();
                tree.setSelectionPath(new TreePath(databaseNode));

                ddlList.add(ExportDDLDTO.builder().database((DatabaseNode) tableNode.getParent()).table(tableNode).build());
            }
        }

        ddlList.sort(Comparator.comparing(ddl -> ddl.getTable().getTableName()));
        new DatabaseExportDDL(project, DataContext.getInstance(project).getSelectDatabase(), ddlList).show();
    }
}
