package com.qihoo.finance.lowcode.declarative.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.declarative.DiffResult;
import com.qihoo.finance.lowcode.common.entity.dto.declarative.DiffTable;
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import com.qihoo.finance.lowcode.declarative.entity.DiffTableNode;
import com.qihoo.finance.lowcode.design.entity.DatabaseNode;

import java.util.HashMap;
import java.util.Map;

/**
 * DeclarativeUtils
 *
 * @author fengjinfu-jk
 * date 2024/4/25
 * @version 1.0.0
 * @apiNote DeclarativeUtils
 */
public class DeclarativeUtils extends LowCodeAppUtils {
    private static final TypeReference<Result<DiffResult>> DIFF_RESULT = new TypeReference<>() {
    };
    private static final TypeReference<Result<DiffTable>> DIFF_TABLE_RESULT = new TypeReference<>() {
    };

    public static Result<DiffResult> diff(Map<String, String> declaresDBTables, Map<String, String> declareDB_actualDB, String tableIgnore) {
        String url = Constants.Url.POST_DECLARATIVE_ANALYZE_DIFF;
        Map<String, Object> param = new HashMap<>();
        param.put("declaresDBTables", declaresDBTables);
        param.put("declareDB_actualDB", declareDB_actualDB);
        param.put("tableIgnore", tableIgnore);

        return catchException(url, () -> RestTemplateUtil.post(url, param, new HashMap<>(), DIFF_RESULT),
                "声明式SQL变更差异查询" + ADD_NOTIFY, false);
    }

    public static DiffTable diffTable(DiffTableNode tableNode, String declareDDL) {
        String url = Constants.Url.POST_DECLARATIVE_ANALYZE_DIFF_TABLE;
        Map<String, Object> param = new HashMap<>();
        param.put("databaseName", tableNode.getDatabaseName());
        DatabaseNode actualDatabase = tableNode.getDatabase().getActualDatabase();
        param.put("actualDatabaseName", actualDatabase.getDatabaseWithInstance());
        param.put("tableName", tableNode.getTableName());
        param.put("declarativeDDL", declareDDL);
        Result<DiffTable> result = catchException(url, () -> RestTemplateUtil.post(url, param, new HashMap<>(), DIFF_TABLE_RESULT),
                "声明式SQL变更差异查询" + ADD_NOTIFY, false);

        return resultData(result, new DiffTable());
    }
}
