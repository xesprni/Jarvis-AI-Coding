package com.qihoo.finance.lowcode.design.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.intellij.openapi.ui.Messages;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.constants.ServiceErrorCode;
import com.qihoo.finance.lowcode.common.entity.CustomResult;
import com.qihoo.finance.lowcode.common.entity.base.PageDTO;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.*;
import com.qihoo.finance.lowcode.common.entity.enums.OrderBy;
import com.qihoo.finance.lowcode.common.exception.ServiceException;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.InnerCacheUtils;
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import com.qihoo.finance.lowcode.common.utils.BeanUtils;
import com.qihoo.finance.lowcode.design.entity.*;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * App接口请求内部工具类
 *
 * @author fengjinfu-jk
 * date 2023/8/7
 * @version 1.0.0
 * @apiNote LowCodeAppService
 */
public class DatabaseDesignUtils extends LowCodeAppUtils {
    protected static final String DEFAULT_DB_TYPE = Constants.DataSource.MySQL;

    // ~ database design
    //------------------------------------------------------------------------------------------------------------------
    private static final TypeReference<Result<Boolean>> BOOLEAN = new TypeReference<>() {
    };
    private static final TypeReference<CustomResult<SQLBatchExecuteResult>> BATCH_EXECUTE_CONSOLE_SQL = new TypeReference<>() {
    };
    private static final TypeReference<CustomResult<String>> STRING = new TypeReference<>() {
    };
    private static final TypeReference<Result<List<PermissionTreeDTO>>> PERMISSION_LIST = new TypeReference<>() {
    };
    private static final TypeReference<Result<List<SQLExecuteHistory>>> SQL_HISTORY = new TypeReference<>() {
    };
    private static final TypeReference<Result<PageDTO<MysqlTableDTO>>> MYSQL_TABLE_PAGE = new TypeReference<>() {
    };
    private static final TypeReference<Result<PageDTO<MongoTableDTO>>> MONGO_TABLE_PAGE = new TypeReference<>() {
    };
    private static final TypeReference<Result<PageDTO<MysqlColumnsDTO>>> COLUMN_PAGE = new TypeReference<>() {
    };
    private static final TypeReference<Result<PageDTO<MysqlIndexDTO>>> INDEX_PAGE = new TypeReference<>() {
    };
    private static final TypeReference<Result<PageDTO<TableChangeRecordDTO>>> DDL_RECORD_PAGE = new TypeReference<>() {
    };
    private static final TypeReference<Result<String>> CREATE_TABLE_DDL = new TypeReference<>() {
    };
    private static final TypeReference<Result<PageDTO<MongoCollectionRow>>> MONGO_PAGE = new TypeReference<>() {
    };

    // ~ database design
    //------------------------------------------------------------------------------------------------------------------

    @NotNull
    public static List<String> getPrimaryKeys(SQLExecuteResult data) {
        List<DatabaseColumnNode> columnNodes = DatabaseDesignUtils.queryDatabaseColumnNodes(Constants.DataSource.MySQL, data.getDatabaseName(), data.getTableName(), new HashMap<>() {{
            put("instanceName", data.getInstanceName());
        }});
        return columnNodes.stream().filter(DatabaseColumnNode::isPK).map(DatabaseColumnNode::getFieldName).collect(Collectors.toList());
    }

    @NotNull
    public static List<DatabaseColumnNode> getColumnInfos(SQLExecuteResult data) {
        return DatabaseDesignUtils.queryDatabaseColumnNodes(Constants.DataSource.MySQL, data.getDatabaseName(), data.getTableName(), new HashMap<>() {{
            put("instanceName", data.getInstanceName());
        }});
    }

    public static PageDTO<TableChangeRecordDTO> queryDdlRecord(DatabaseNode databaseNode, MySQLTableNode tableNode, int page, int pageSize) {
        // dataSourceType=mysql&databaseName=lingxi_server&tableName=test2&instanceName=dev_2490&page=1&size=3
        Map<String, Object> param = new HashMap<>();
        param.put("page", String.valueOf(page));
        param.put("pageSize", String.valueOf(pageSize));
        param.put("size", String.valueOf(pageSize));

        param.put("dataSourceType", StringUtils.defaultString(databaseNode.getDataSourceType(), DEFAULT_DB_TYPE));

        param.put("database", tableNode.getDatabase());
        param.put("databaseName", tableNode.getDatabase());

        param.put("table", tableNode.getTableName());
        param.put("tableName", tableNode.getTableName());

        Map<String, Object> nodeAttr = databaseNode.getNodeAttr();
        if (MapUtils.isNotEmpty(nodeAttr)) {
            param.putAll(nodeAttr);
        }

        String url = Constants.Url.GET_DDL_RECORD;
        Result<PageDTO<TableChangeRecordDTO>> result = catchException(url, () -> RestTemplateUtil.get(url, param, new HashMap<>(), DDL_RECORD_PAGE), "查询表变更记录失败" + ADD_NOTIFY);
        return resultData(result, PageDTO.empty());
    }

    public static Result<Boolean> validateSQL(String dataSourceType, String database, String table, String sqlScriptStr, Map<String, Object> nodeAttr) {
        Map<String, Object> param = new HashMap<>();
        param.put("sqlScriptStr", sqlScriptStr);
        param.put("databaseName", database);
        param.put("tableName", table);
        if (MapUtils.isNotEmpty(nodeAttr)) {
            param.putAll(nodeAttr);
        }
        String url = Constants.Url.POST_VALIDATE_SQL;
        return catchException(url, () -> RestTemplateUtil.post(url, param, new HashMap<>(), BOOLEAN), "SQL校验请求失败" + ADD_NOTIFY, false);
    }

    public static Result<SQLBatchExecuteResult> batchCheckAndExecuteSQL(Map<String, String> headers, String dataSourceType, String database, String sqlScriptStr, Map<String, Object> nodeAttr, long page, long pageSize) {
        String url = setUrlPath(Constants.Url.POST_BATCH_EXECUTE_CONSOLE_SQL, "datasourceType", dataSourceType);
        Map<String, Object> param = new HashMap<>();
        param.put("databaseName", database);
        param.put("sqlScriptStr", sqlScriptStr);

        // plugins端指定的分页参数-页容量
        param.put("pageSize", String.valueOf(pageSize));
        param.put("page", String.valueOf(page));
        if (MapUtils.isNotEmpty(nodeAttr)) {
            param.putAll(nodeAttr);
        }

        return handleException(url, () -> RestTemplateUtil.post(url, param, headers, BATCH_EXECUTE_CONSOLE_SQL, 60000), (ex, rs) -> {
            if (ex instanceof ServiceException) {
                if (ServiceErrorCode.SOCKET_TIMEOUT.getCode().equals(((ServiceException) ex).getErrorCode())) {
                    // 响应超时
                    String msg = "SQL执行耗时较长, 自动变更为异步线程执行, 请稍后查看执行结果并自行验证";
                    rs.setErrorCode(ServiceErrorCode.SOCKET_TIMEOUT.getCode());
                    rs.setErrorMsg(msg);
                    rs.setSuccess(true);

                    // 包装为执行概览信息
                    SQLBatchExecuteResult result = new SQLBatchExecuteResult();
                    result.setResultOverview(Lists.newArrayList(SQLBatchExecuteResult.ResultItem.builder().sql(sqlScriptStr).message(msg).success(false).build()));
                    rs.setData(result);
                }
            }
        }, "批量SQL执行请求失败" + ADD_NOTIFY, false);
    }

    public static Result<SQLBatchExecuteResult> batchExecuteConsoleSQL(DatabaseNode database, String sqlScriptStr, long page, long pageSize) {
        return batchCheckAndExecuteSQL(new HashMap<>(), database.getDataSourceType(), database.getName(), sqlScriptStr, database.getNodeAttr(), page, pageSize);
    }

    public static Result<SQLBatchExecuteResult> batchExecuteConsoleSQL(DatabaseNode database, String sqlScriptStr) {
        return batchCheckAndExecuteSQL(new HashMap<>(), database.getDataSourceType(), database.getName(), sqlScriptStr, database.getNodeAttr(), 1, 1);
    }

    public static Result<SQLBatchExecuteResult> batchExecuteConsoleSQL(Map<String, String> headers, DatabaseNode database, String sqlScriptStr) {
        return batchCheckAndExecuteSQL(headers, database.getDataSourceType(), database.getName(), sqlScriptStr, database.getNodeAttr(), 1, 1);
    }

    public static boolean getExecuteResult(Result<SQLBatchExecuteResult> result, String tips) {
        if (result.isFail() || !result.isSuccess()) {
            String errMsg = "";
            if (StringUtils.isNotEmpty(result.getErrorMsg())) {
                String[] split = result.getErrorMsg().split(";");
                if (split.length > 0) {
                    errMsg = split[split.length - 1];
                }
            }

            Messages.showMessageDialog(tips + " \n\n" + errMsg, "", Icons.scaleToWidth(Icons.FAIL, 60));
            return false;
        }

        String failViewText = result.getData().getResultOverview().stream().filter(rs -> !rs.isSuccess()).map(SQLBatchExecuteResult.ResultItem::getMessage).collect(Collectors.joining("\n\n"));
        if (StringUtils.isNotEmpty(failViewText)) {
            Messages.showMessageDialog(tips + " \n\n" + failViewText, "", Icons.scaleToWidth(Icons.FAIL, 60));
            return false;
        }

        return true;
    }

    // ~ database
    //------------------------------------------------------------------------------------------------------------------

    public static List<MysqlColumnsDTO> queryTableColumnList(String userNo, String dataSourceType, String database, String table, Map<String, Object> nodeAttr) {
        List<MysqlColumnsDTO> resRows = new ArrayList<>();
        List<MysqlColumnsDTO> tempRows;
        final int batchSize = 100;
        int page = 1;
        do {
            tempRows = queryTableColumnPage(userNo, dataSourceType, database, table, nodeAttr, page, batchSize).getRows();
            page++;
            resRows.addAll(tempRows);
        } while (tempRows.size() == batchSize);

        return resRows;
    }

    private static PageDTO<MysqlColumnsDTO> queryTableColumnPage(String userNo, String dataSourceType, String database, String table, Map<String, Object> nodeAttr, int page, int size) {
        Map<String, Object> param = new HashMap<>();
        param.put("page", String.valueOf(page));
        param.put("size", String.valueOf(size));
        param.put("pageSize", String.valueOf(size));

        param.put("userNo", userNo);
        param.put("dataSourceType", StringUtils.defaultString(dataSourceType, DEFAULT_DB_TYPE));
        param.put("database", database);
        param.put("table", table);
        param.put("databaseName", database);
        param.put("tableName", table);

        if (MapUtils.isNotEmpty(nodeAttr)) {
            param.putAll(nodeAttr);
        }

        String cacheKey = "@COLUMN_" + JSON.toJson(param);
        Result<PageDTO<MysqlColumnsDTO>> cache = InnerCacheUtils.getCache(cacheKey, COLUMN_PAGE);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        String url = Constants.Url.GET_QUERY_TABLE_COLUMNS;
        Result<PageDTO<MysqlColumnsDTO>> result = catchException(url, () -> RestTemplateUtil.get(url, param, new HashMap<>(), COLUMN_PAGE), "加载表字段失败" + ADD_NOTIFY, false);

        if (result.isSuccess()) InnerCacheUtils.setCache(cacheKey, JSON.toJson(result));
        return resultData(result, PageDTO.empty());
    }

    public static List<MysqlIndexDTO> queryTableIndexList(String userNo, String dataSourceType, String database, String table, Map<String, Object> nodeAttr) {
        List<MysqlIndexDTO> resRows = new ArrayList<>();
        List<MysqlIndexDTO> tempRows;
        final int batchSize = 100;
        int page = 1;
        do {
            tempRows = queryTableIndexPage(userNo, dataSourceType, database, table, nodeAttr, page, batchSize).getRows();
            page++;
            resRows.addAll(tempRows);
        } while (tempRows.size() == batchSize);

        return resRows;
    }

    private static PageDTO<MysqlIndexDTO> queryTableIndexPage(String userNo, String dataSourceType, String database, String table, Map<String, Object> nodeAttr, int page, int size) {
        Map<String, Object> param = new HashMap<>();
        param.put("page", String.valueOf(page));
        param.put("size", String.valueOf(size));
        param.put("pageSize", String.valueOf(size));

        param.put("userNo", userNo);
        param.put("dataSourceType", StringUtils.defaultString(dataSourceType, DEFAULT_DB_TYPE));
        param.put("database", database);
        param.put("table", table);
        param.put("databaseName", database);
        param.put("tableName", table);

        if (MapUtils.isNotEmpty(nodeAttr)) {
            param.putAll(nodeAttr);
        }

        String cacheKey = "@INDEX_" + JSON.toJson(param);
        Result<PageDTO<MysqlIndexDTO>> cache = InnerCacheUtils.getCache(cacheKey, INDEX_PAGE);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        String url = Constants.Url.GET_QUERY_TABLE_INDEXES;
        Result<PageDTO<MysqlIndexDTO>> result = catchException(url, () -> RestTemplateUtil.get(url, param, new HashMap<>(), INDEX_PAGE), "加载表索引信息失败" + ADD_NOTIFY, false);

        if (result.isSuccess()) InnerCacheUtils.setCache(cacheKey, JSON.toJson(result));
        return resultData(result, PageDTO.empty());
    }

    private static <T extends AuthTableDTO> List<T> queryUserPermissionTableList(String userNo, String dataSourceType, String database, Map<String, Object> nodeAttr, TypeReference<Result<PageDTO<T>>> typeReference) {
        List<T> resRows = new ArrayList<>();
        List<T> tempRows;
        final int batchSize = 1000;
        int page = 1;
        do {
            tempRows = queryUserPermissionTablePage(userNo, dataSourceType, database, nodeAttr, page, batchSize, typeReference).getRows();
            page++;
            resRows.addAll(tempRows);
        } while (tempRows.size() == batchSize);

        return resRows;
    }

    private static <T extends AuthTableDTO> PageDTO<T> queryUserPermissionTablePage(String userNo,
                                                                                    String dataSourceType,
                                                                                    String database, Map<String, Object> nodeAttr,
                                                                                    int page,
                                                                                    int size,
                                                                                    TypeReference<Result<PageDTO<T>>> typeReference) {
        String url = setUrlPath(Constants.Url.GET_QUERY_PERMISSION_TABLES, "datasourceType", dataSourceType);
        Map<String, Object> param = new HashMap<>();
        param.put("page", String.valueOf(page));
        param.put("size", String.valueOf(size));
        param.put("pageSize", String.valueOf(size));

        dataSourceType = StringUtils.defaultString(dataSourceType, DEFAULT_DB_TYPE);
        param.put("userNo", userNo);
        param.put("dataSourceType", dataSourceType);
        param.put("database", database);
        param.put("databaseName", database);
        param.put("orderBy", getUserContext().orderBy);

        if (MapUtils.isNotEmpty(nodeAttr)) {
            param.putAll(nodeAttr);
        }

        String cacheKey = "@TABLE_" + url + JSON.toJson(param);
        Result<PageDTO<T>> cache = InnerCacheUtils.getCache(cacheKey, typeReference);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        Result<PageDTO<T>> result = catchException(url,
                () -> RestTemplateUtil.get(url, param, new HashMap<>(), typeReference), "加载数据库表信息失败" + ADD_NOTIFY, false);

        if (result.isSuccess()) InnerCacheUtils.setCache(cacheKey, JSON.toJson(result));
        return resultData(result, PageDTO.empty());
    }

    public static List<PermissionTreeDTO> queryUserPermissionList(String userNo, String datasourceType) {
        String url = setUrlPath(Constants.Url.GET_QUERY_PERMISSION_TREE, "datasourceType", datasourceType);

        Map<String, Object> param = new HashMap<>();
        param.put("userNo", userNo);
        String cacheKey = "@PERMISSION_" + url + JSON.toJson(param);

        Result<List<PermissionTreeDTO>> cache = InnerCacheUtils.getCache(cacheKey, PERMISSION_LIST);
        if (Objects.nonNull(cache)) {
            return resultData(cache);
        }

        Result<List<PermissionTreeDTO>> result = catchException(url, () -> RestTemplateUtil.get(url, param, new HashMap<>(), PERMISSION_LIST), "加载用户权限数据库信息失败" + ADD_NOTIFY, false);

        if (result.isSuccess()) InnerCacheUtils.setCache(cacheKey, JSON.toJson(result));
        return resultData(result, new ArrayList<>());
    }

    public static List<SQLExecuteHistory> getSQLExecuteHistory(int page, int pageSize, String searchKey) {
        String url = setUrlPath(Constants.Url.GET_QUERY_SQL_HISTORY, "datasourceType", Constants.DataSource.MySQL);
        Map<String, Object> param = new HashMap<>();
        param.put("page", page);
        param.put("pageSize", pageSize);
        param.put("searchKey", searchKey);
        Result<List<SQLExecuteHistory>> result = catchException(url, () -> RestTemplateUtil.get(url, param, new HashMap<>(), SQL_HISTORY), "加载用户历史SQL查询记录失败" + ADD_NOTIFY, false);

        return resultData(result, new ArrayList<>());
    }

    public static List<SQLExecuteHistory> getSQLExecuteHistory10() {
        return getSQLExecuteHistory(1, 10, "");
    }

    public static List<SQLExecuteHistory> getSQLExecuteHistory100(String searchKey) {
        return getSQLExecuteHistory(1, 100, searchKey);
    }

    public static Result<List<SQLExecuteHistory>> saveSQLExecuteHistories(String datasourceType, SQLExecuteHistory history) {
        String url = setUrlPath(Constants.Url.POST_SAVE_SQL_HISTORY, "datasourceType", datasourceType);
        return catchException(url, () -> RestTemplateUtil.post(url, history, new HashMap<>(), SQL_HISTORY), "保存用户历史SQL查询记录失败" + ADD_NOTIFY, false);
    }

    public static Boolean deleteSQLExecuteHistory(SQLExecuteHistory history) {
        String url = setUrlPath(Constants.Url.POST_DELETE_SQL_HISTORY, "datasourceType", Constants.DataSource.MySQL);
        Map<String, Object> param = new HashMap<>();
        param.put("id", history.getId());
        Result<?> result = catchException(url, () -> RestTemplateUtil.post(url, param, new HashMap<>(), BOOLEAN), "删除用户历史SQL查询记录失败" + ADD_NOTIFY, false);

        return result.isSuccess();
    }

    public static List<MongoCollectionNode> queryMongoCollectionNodes(MongoDatabaseNode database) {
        List<MongoCollectionNode> nodes = new ArrayList<>();
        List<MongoTableDTO> tables = DatabaseDesignUtils.queryUserPermissionTableList(
                getUserInfo().getUserNo(), Constants.DataSource.MongoDB, database.getName(), database.getNodeAttr(), MONGO_TABLE_PAGE
        );
        for (MongoTableDTO table : tables) {
            MongoCollectionNode node = new MongoCollectionNode();
            node.setTableName(table.getCollectionName());
            node.setDatabase(database.getName());
            node.setCreateTime(ObjectUtils.defaultIfNull(table.getCreateTime(), new Date(0)));
            node.setUpdateTime(ObjectUtils.defaultIfNull(table.getUpdateTime(), node.getCreateTime()));

            nodes.add(node);
        }

        sortTables(nodes, OrderBy.valueOf(StringUtils.defaultString(getUserContext().orderBy, OrderBy.UPDATE_TIME_DESC.name())));
        return nodes;
    }

    private static List<MySQLTableNode> queryMySQLTableNodes(String database, Map<String, Object> nodeAttr) {
        List<MySQLTableNode> nodes = new ArrayList<>();
        List<MysqlTableDTO> tables = DatabaseDesignUtils.queryUserPermissionTableList(
                getUserInfo().getUserNo(), Constants.DataSource.MySQL, database, nodeAttr, MYSQL_TABLE_PAGE
        );
        for (MysqlTableDTO table : tables) {
            MySQLTableNode node = new MySQLTableNode();
            node.setPreName(StringUtils.EMPTY);
            node.setTableName(table.getTableName());
            node.setTableComment(table.getTableComment());
            node.setDatabase(database);
            node.setCharset(table.getTableCharset());
            node.setCreateTime(ObjectUtils.defaultIfNull(table.getCreateTime(), new Date(0)));
            node.setUpdateTime(ObjectUtils.defaultIfNull(table.getUpdateTime(), node.getCreateTime()));

            nodes.add(node);
        }

        return nodes;
    }

    public static List<MySQLTableNode> queryMySQLTableNodes(DatabaseNode database) {
        List<MySQLTableNode> tableNodes = queryMySQLTableNodes(database.getName(), database.getNodeAttr());
        sortTables(tableNodes, OrderBy.valueOf(StringUtils.defaultString(getUserContext().orderBy, OrderBy.UPDATE_TIME_DESC.name())));
        return tableNodes;
    }

    public static void sortTables(List<? extends AbstractTableNode> tableNodes, OrderBy orderBy) {
        switch (orderBy) {
            case TABLE_NAME_ASC:
                tableNodes.sort(Comparator.comparing(AbstractTableNode::getTableName));
                break;
            case TABLE_NAME_DESC:
                tableNodes.sort(Comparator.comparing(AbstractTableNode::getTableName).reversed());
                break;
//            case CREATE_TIME_ASC:
//                tableNodes.sort(Comparator.comparing(DatabaseTableNode::getCreateTime));
//                break;
//            case CREATE_TIME_DESC:
//                tableNodes.sort(Comparator.comparing(DatabaseTableNode::getCreateTime).reversed());
//                break;
            case UPDATE_TIME_ASC:
                tableNodes.sort(Comparator.comparing(AbstractTableNode::getUpdateTime));
                break;
            case UPDATE_TIME_DESC:
                tableNodes.sort(Comparator.comparing(AbstractTableNode::getUpdateTime).reversed());
                break;
        }
    }

    public static List<DatabaseColumnNode> queryDatabaseColumnNodes(String dataSourceType, String database, String tableName, Map<String, Object> nodeAttr) {
        List<DatabaseColumnNode> nodes = new ArrayList<>();
        List<MysqlColumnsDTO> columns = DatabaseDesignUtils.queryTableColumnList(getUserInfo().getUserNo(), dataSourceType, database, tableName, nodeAttr);
        for (MysqlColumnsDTO column : columns) {
            DatabaseColumnNode node = new DatabaseColumnNode();
            node.setFieldName(column.getColumnName());
            node.setFieldComment(column.getColumnComment());
            node.setFieldType(column.getColumnType());
            node.setFieldNo(column.getOrdinalPosition().intValue());
            // fixme: MySQL是通过PRI标识判断主键, 其余数据源未知, 可能需要改动
            node.setPK("PRI".equals(column.getColumnKey()));

//            node.setFieldLength(ObjectUtils.defaultIfNull(ObjectUtils.defaultIfNull(column.getCharacterMaximumLength(), column.getNumericPrecision()), 0L).intValue());
            if (Objects.nonNull(column.getColumnTypeDescLength())) {
                node.setFieldLength(column.getColumnTypeDescLength().intValue());
            }

            if (Objects.nonNull(column.getNumericScale())) {
                node.setFieldPrecision(column.getNumericScale().intValue());
            }
            // column.getIsNullable() {"YES", "NO"}
            node.setNotNull("NO".equals(column.getIsNullable()));
            node.setFieldDefaults(column.getColumnDefault());
            node.setUnsigned(column.getIsUnsigned());
            node.setAutoIncr(column.getIsAutoIncrement());

            nodes.add(node);
        }

        return nodes;
    }

    public static List<DatabaseIndexNode> queryDatabaseIndexNodes(String dataSourceType, String database, String tableName, Map<String, Object> nodeAttr) {
        List<DatabaseIndexNode> nodes = new ArrayList<>();
        List<MysqlIndexDTO> indexList = DatabaseDesignUtils.queryTableIndexList(getUserInfo().getUserNo(), dataSourceType, database, tableName, nodeAttr);
        for (MysqlIndexDTO index : indexList) {
            DatabaseIndexNode indexNode = BeanUtils.copyPropertiesIgnoreNull(index, DatabaseIndexNode::new);
            String indexField = Arrays.stream(indexNode.getColumnSetStr().split(",")).map(c -> "`" + c + "`").collect(Collectors.joining(" ,"));
            indexNode.setIndexField(indexField);
            nodes.add(indexNode);
        }

        return nodes;
    }

    public static String queryTableCreateDDL(DatabaseNode database, MySQLTableNode table) {
        Map<String, Object> param = new HashMap<>();
        String tableName = table.getTableName();
        String databaseName = table.getDatabase();

        param.put("dataSourceType", StringUtils.defaultString(database.getDataSourceType(), DEFAULT_DB_TYPE));
        param.put("database", databaseName);
        param.put("table", tableName);
        param.put("databaseName", databaseName);
        param.put("tableName", tableName);

        Map<String, Object> nodeAttr = database.getNodeAttr();
        if (MapUtils.isNotEmpty(nodeAttr)) {
            param.putAll(nodeAttr);
        }


        String url = Constants.Url.GET_CREATE_TABLE_DDL;
        Result<String> result = catchException(url, () -> RestTemplateUtil.get(url, param, new HashMap<>(), CREATE_TABLE_DDL), "查询表结构DDL失败" + ADD_NOTIFY);
        return resultData(result, StringUtils.EMPTY);
    }

    public static String ddlExportPath() {
        String url = Constants.Url.GET_DDL_EXPORT_PATH;
        Map<String, Object> param = new HashMap<>();

        Result<String> result = catchException(url, () -> RestTemplateUtil.get(url, param, new HashMap<>(), STRING), "ddl-默认导出路径查询失败" + ADD_NOTIFY, false);
        return resultData(result, "material/mysql/");
    }

    public static boolean createMongoCollection(MongoDatabaseNode database, String collection) {
        String url = setUrlPath(Constants.Url.POST_CREATE_MONGO_COLLECTION, "datasourceType", Constants.DataSource.MongoDB);
        Map<String, Object> param = new HashMap<>();
        param.put("databaseName", database.getName());
        param.put("collectionName", collection);
        Map<String, Object> nodeAttr = database.getNodeAttr();
        if (MapUtils.isNotEmpty(nodeAttr)) {
            param.putAll(nodeAttr);
        }

        Result<Boolean> result = catchException(url, () -> RestTemplateUtil.post(url, param, new HashMap<>(), BOOLEAN), "新建collection失败" + ADD_NOTIFY, false);
        return resultData(result, true);
    }

    public static Result<Boolean> dropMongoCollection(MongoDatabaseNode database, String collection) {
        String url = setUrlPath(Constants.Url.POST_DROP_MONGO_COLLECTION, "datasourceType", Constants.DataSource.MongoDB);
        Map<String, Object> param = new HashMap<>();
        param.put("databaseName", database.getName());
        param.put("collectionName", collection);
        Map<String, Object> nodeAttr = database.getNodeAttr();
        if (MapUtils.isNotEmpty(nodeAttr)) {
            param.putAll(nodeAttr);
        }

        return catchException(url, () -> RestTemplateUtil.post(url, param, new HashMap<>(), BOOLEAN), "删除collection失败" + ADD_NOTIFY, false);
    }

    public static PageDTO<MongoCollectionRow> queryCollectionDataPage(MongoDatabaseNode database, MongoCollectionNode collectionNode, String filterBson, int page, int pageSize) {
        Map<String, Object> param = new HashMap<>();
        param.put("page", page);
        param.put("size", pageSize);
        param.put("pageSize", pageSize);

        param.put("databaseName", database.getName());
        param.put("collectionName", collectionNode.getTableName());
        Map<String, Object> nodeAttr = database.getNodeAttr();
        if (MapUtils.isNotEmpty(nodeAttr)) {
            param.putAll(nodeAttr);
        }

        String url = setUrlPath(Constants.Url.GET_MONGO_COLLECTION_PAGE, "datasourceType", Constants.DataSource.MongoDB);
        Result<PageDTO<MongoCollectionRow>> result = catchException(url, () -> RestTemplateUtil.get(url, param, new HashMap<>(), MONGO_PAGE), "查询mongo集合数据失败" + ADD_NOTIFY);
        return resultData(result, PageDTO.empty());
    }

    public static PageDTO<MongoCollectionRow> queryCollectionByBson(DatabaseNode database, MongoCollectionNode collectionNode, String filterBson, int page, int pageSize) {
        Map<String, Object> param = new HashMap<>();
        param.put("filterBson", filterBson);
        param.put("pageSize", pageSize);
        param.put("size", pageSize);
        param.put("page", page);

        param.put("databaseName", database.getName());
        param.put("collectionName", collectionNode.getTableName());
        Map<String, Object> nodeAttr = database.getNodeAttr();
        if (MapUtils.isNotEmpty(nodeAttr)) {
            param.putAll(nodeAttr);
        }

        String url = setUrlPath(Constants.Url.GET_MONGO_COLLECTION_BSON, "datasourceType", Constants.DataSource.MongoDB);
        Result<PageDTO<MongoCollectionRow>> result = catchException(url, () -> RestTemplateUtil.post(url, param, new HashMap<>(), MONGO_PAGE), "查询mongo集合数据失败" + ADD_NOTIFY);
        return resultData(result, PageDTO.empty());
    }
}
