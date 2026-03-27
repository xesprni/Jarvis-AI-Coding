package com.qihoo.finance.lowcode.design.ui;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.ui.base.TreePanelAdapter;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.design.ui.tree.DatabaseTreePanel;
import com.qihoo.finance.lowcode.design.ui.tree.MongoTreePanel;
import com.qihoo.finance.lowcode.design.ui.tree.MultiDatasourceTreePanel;
import com.qihoo.finance.lowcode.design.ui.tree.MySQLTreePanel;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * DatabaseTreeFactory
 *
 * @author fengjinfu-jk
 * date 2023/12/22
 * @version 1.0.0
 * @apiNote DatabaseTreeFactory
 */
public class DatabaseTreePanelFactory extends DatabaseBasePanel {
    private final Project project;

    public DatabaseTreePanelFactory(@NotNull Project project) {
        super(project);
        this.project = project;
    }

    @Override
    public Component createPanel() {
        return null;
    }

    public DatabaseTreePanel databaseTreePanel(){
        // 单数据源切换展示
         return singleDatabaseTreePanel();

        // 多数据源合并展示
//        return multiDatabaseTreePanel();
    }

    public DatabaseTreePanel databaseTreePanel(String datasourceType){
        // 单数据源切换展示
         return singleDatabaseTreePanel(datasourceType);

        // 多数据源合并展示
//        return multiDatabaseTreePanel();
    }

    public DatabaseTreePanel singleDatabaseTreePanel() {
        String datasourceType = StringUtils.defaultString(DataContext.getInstance(project).getDatasourceType(), Constants.DataSource.MySQL);
        if (datasourceType.equals(Constants.DataSource.MongoDB)) {
            return project.getService(MongoTreePanel.class);
        }

        return project.getService(MySQLTreePanel.class);
    }

    public DatabaseTreePanel singleDatabaseTreePanel(String datasourceType) {
        if (datasourceType.equals(Constants.DataSource.MongoDB)) {
            return project.getService(MongoTreePanel.class);
        }

        return project.getService(MySQLTreePanel.class);
    }

    public DatabaseTreePanel multiDatabaseTreePanel() {
        return project.getService(MultiDatasourceTreePanel.class);
    }

    public JComponent switchDatabaseView() {
        DatabaseTreePanel databaseTreePanel = databaseTreePanel();
        return databaseTreePanel.getPanel();
    }

    public JComponent switchDatabaseView(String datasourceType) {
        DatabaseTreePanel databaseTreePanel = databaseTreePanel(datasourceType);
        return databaseTreePanel.getPanel();
    }
}
