package com.qihoo.finance.lowcode.common.util;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.apitrack.entity.ApiGroupNode;
import com.qihoo.finance.lowcode.apitrack.entity.ApiNode;
import com.qihoo.finance.lowcode.apitrack.entity.ApplicationNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvOrgNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvRepoNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvSprintNode;
import com.qihoo.finance.lowcode.codereview.entity.CodeRvTaskNode;
import com.qihoo.finance.lowcode.design.entity.*;
import com.qihoo.finance.lowcode.design.util.DatabaseDesignUtils;
import lombok.Data;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓存数据工具类
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
public class DataContext {

    /**
     * 单例模式
     */
    public static DataContext getInstance(Project project) {
        return project.getService(DataContext.class);
    }

    private DataContext() {
    }

    /**
     * 当前选中的数据库树
     */
    private JTree dbTree;

    private String datasourceType;

    /**
     * 当前选中的api树
     */
    private JTree apiTree;

    /**
     * 当前选中的codeReview树
     */
    private JTree codeRvTree;

    private CodeRvOrgNode selectOrgNode;
    private CodeRvRepoNode selectCodeRvRepo;
    private CodeRvSprintNode selectCodeRvSprint;
    private CodeRvTaskNode selectCodeRvTask;

    /**
     * 当前选中的数据库
     */
    private DatabaseNode selectDatabase;
    private MongoDatabaseNode selectMongoDatabase;
    private MongoCollectionNode selectMongoCollection;

    /**
     * 当前选中的表
     */
    private MySQLTableNode selectDbTable;

    /**
     * 所有选中的表
     */
    private List<MySQLTableNode> dbTableList;

    private List<DatabaseNode> allMySQLDatabaseList;
    private List<MongoDatabaseNode> allMongoDatabaseList;

    private Map<String, DatabaseNode> consoleDatabase = new HashMap<>();
    private Map<String, List<String>> consoleTableColumns = new HashMap<>();

    /**
     * 数据库表生成代码模块
     */
    private String selectTableGenModule;

    /**
     * 接口代码生成模块
     */
    private String selectApiGenModule;

    /**
     * 数据库代码生成模块
     */
    private String selectDBGenModule;

    private boolean mustSyncLoadDbTree = false;

    public DatabaseNode getConsoleDatabase(String fileName) {
        return consoleDatabase.get(fileName);
    }

    public void removeConsoleDatabase(String fileName) {
        consoleDatabase.remove(fileName);
    }

    public void setConsoleDatabase(String fileName, DatabaseNode databaseNode) {
        this.consoleDatabase.put(fileName, databaseNode);
    }

    /**
     * 选中API
     */
    private ApplicationNode selectApplicationNode;
    private ApiGroupNode selectApiGroupNode;
    private List<ApiGroupNode> selectApiGroupNodes = new ArrayList<>();
    private ApiNode selectApiNode;

    public MySQLTableNode getSelectDbTable(boolean getCacheIfExist) {
        MySQLTableNode tableNode = getSelectDbTable();
        if (getCacheIfExist) {
            return tableNode;
        }

        DatabaseNode nameSpace = getSelectDatabase();
        loadTableWithoutCache(nameSpace, tableNode);

        return tableNode;
    }

    public List<MySQLTableNode> getDbTableList(boolean getCacheIfExist) {
        List<MySQLTableNode> tableList = getDbTableList();
        if (getCacheIfExist) {
            return tableList;
        }

        DatabaseNode nameSpace = getSelectDatabase();
        for (MySQLTableNode tableNode : tableList) {
            loadTableWithoutCache(nameSpace, tableNode);
        }

        return tableList;
    }

    private void loadTableWithoutCache(DatabaseNode nameSpace, MySQLTableNode tableNode) {
        CacheManager.refreshInnerCache();

        JTreeLoadingUtils.loading(false, false, dbTree, tableNode, () -> {
            List<? extends DefaultMutableTreeNode> nodes;
            // 编辑则重新加载表
            nodes = DatabaseDesignUtils.queryDatabaseColumnNodes(nameSpace.getDataSourceType(), nameSpace.getCode(), tableNode.getTableName(), nameSpace.getNodeAttr());
            // 索引重置
            List<DatabaseIndexNode> indexNodes = DatabaseDesignUtils.queryDatabaseIndexNodes(nameSpace.getDataSourceType(), nameSpace.getCode(), selectDbTable.getTableName(), nameSpace.getNodeAttr());
            tableNode.setIndexList(indexNodes);

            tableNode.removeAllChildren();
            nodes.forEach(tableNode::add);
            return nodes;
        });
    }
}
