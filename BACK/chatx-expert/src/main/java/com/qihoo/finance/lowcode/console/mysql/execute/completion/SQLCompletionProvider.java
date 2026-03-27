package com.qihoo.finance.lowcode.console.mysql.execute.completion;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JTreeLoadingUtils;
import com.qihoo.finance.lowcode.console.completion.CompletionKeywords;
import com.qihoo.finance.lowcode.console.completion.CompletionProvider;
import com.qihoo.finance.lowcode.design.entity.DatabaseColumnNode;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.*;

/**
 * SQLCompletionProvider
 *
 * @author fengjinfu-jk
 * date 2024/1/25
 * @version 1.0.0
 * @apiNote SQLCompletionProvider
 */
public class SQLCompletionProvider extends CompletionProvider {

    @Override
    protected void addCompletionVariants(@NotNull String text, int offset, @NotNull String prefix, @NotNull CompletionResultSet result) {
        // sql keyword
        Project project = ProjectUtils.getCurrProject();
        if (Objects.isNull(project)) return;
        FileEditor selectedEditor = FileEditorManager.getInstance(project).getSelectedEditor();
        if (Objects.isNull(selectedEditor)) return;

        mysqlLookup(result, project, selectedEditor.getFile().getName());
    }

    public static void mysqlLookup(@NotNull CompletionResultSet result, Project project, String fileName) {
        // sql keyword
        result.addAllElements(CompletionKeywords.MysqlLookup);

        // schema
        DataContext dataContext = DataContext.getInstance(project);
        DatabaseNode database = dataContext.getConsoleDatabase(fileName);
        if (Objects.nonNull(database)) {
            // schema
            result.addElement(LookupElementBuilder.create(database.getName()).withPresentableText(database.getName()).withIcon(Icons.scaleToWidth(Icons.DB_BLOCK, 18)).withTypeText("schema").bold());

            // tableName
            if (database.getChildCount() > 1) {
                for (Enumeration<? extends TreeNode> children = database.children(); children.hasMoreElements(); ) {
                    TreeNode treeNode = children.nextElement();
                    if (treeNode instanceof MySQLTableNode) {
                        MySQLTableNode tableNode = (MySQLTableNode) treeNode;
                        String tableName = tableNode.getTableName();
                        result.addElement(LookupElementBuilder.create(tableName).withPresentableText(tableName).withIcon(Icons.scaleToWidth(Icons.TABLE2, 16)).withTypeText("table").bold());
                        result.addElement(LookupElementBuilder.create(database.getName() + "." + tableName).withPresentableText(database.getName() + "." + tableName).withIcon(Icons.scaleToWidth(Icons.TABLE2, 16)).withTypeText("table").bold());

                        // columns
                        if (tableNode.getChildCount() > 1) {
                            for (Enumeration<? extends TreeNode> tableChildren = tableNode.children(); tableChildren.hasMoreElements(); ) {
                                TreeNode columnNode = tableChildren.nextElement();
                                if (columnNode instanceof DatabaseColumnNode) {
                                    String columnName = ((DatabaseColumnNode) columnNode).getFieldName();
                                    result.addElement(LookupElementBuilder.create(columnName).withPresentableText(columnName).withIcon(Icons.scaleToWidth(Icons.COLUMN, 16)).withTypeText("column").bold());
                                    result.addElement(LookupElementBuilder.create(tableName + "." + columnName).withPresentableText(tableName + "." + columnName).withIcon(Icons.scaleToWidth(Icons.COLUMN, 16)).withTypeText("column").bold());
                                }
                            }
                        }
                    }
                }
            } else {
                JTreeLoadingUtils.loading(true, dataContext.getDbTree(), database, () -> DatabaseDesignUtils.queryMySQLTableNodes(database), tables -> {
                    for (DefaultMutableTreeNode table : tables) {
                        if (table instanceof MySQLTableNode) {
                            String tableName = ((MySQLTableNode) table).getTableName();
                            result.addElement(LookupElementBuilder.create(tableName).withPresentableText(tableName).withIcon(Icons.scaleToWidth(Icons.TABLE2, 16)).withTypeText("table").bold());
                            result.addElement(LookupElementBuilder.create(database.getName() + "." + tableName).withPresentableText(database.getName() + "." + tableName).withIcon(Icons.scaleToWidth(Icons.TABLE2, 16)).withTypeText("table").bold());
                        }
                    }
                });
            }

            // columns
            Map<String, List<String>> consoleTableColumns = dataContext.getConsoleTableColumns();
            List<String> columns = consoleTableColumns.getOrDefault(fileName, new ArrayList<>());
            for (String column : columns) {
                result.addElement(LookupElementBuilder.create(column).withPresentableText(column).withIcon(Icons.scaleToWidth(Icons.COLUMN, 16)).withTypeText("result column").bold());
            }

            // table
//                if (database.getChildCount() > 0) {
//                    for (Enumeration<? extends TreeNode> e = database.children(); e.hasMoreElements(); ) {
//                        TreeNode child = e.nextElement();
//                        if (child instanceof DatabaseTableNode) {
//                            String tableName = ((DatabaseTableNode) child).getTableName();
//                            result.addElement(LookupElementBuilder.create(tableName).withPresentableText(tableName)
//                                    .withIcon(Icons.scaleToWidth(Icons.TABLE2, 16)).withTypeText("table").bold());
//                        }
//                    }
//                }
        }
    }
}
