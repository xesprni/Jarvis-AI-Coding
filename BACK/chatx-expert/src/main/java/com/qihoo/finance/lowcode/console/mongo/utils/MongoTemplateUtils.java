package com.qihoo.finance.lowcode.console.mongo.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import com.qihoo.finance.lowcode.design.entity.MongoDatabaseNode;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import com.qihoo.finance.lowcode.console.mongo.model.MongoQueryOptions;
import com.qihoo.finance.lowcode.console.mongo.view.model.MongoCollectionResult;
import org.apache.commons.collections.MapUtils;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.HashMap;
import java.util.Map;

/**
 * MongoTemplateUtils
 *
 * @author fengjinfu-jk
 * date 2024/1/19
 * @version 1.0.0
 * @apiNote MongoTemplateUtils
 */
public class MongoTemplateUtils extends LowCodeAppUtils {
    private static final TypeReference<Result<MongoCollectionResult>> MONGO_COLLECTION_RESULT = new TypeReference<>() {
    };

    private static void setObjectId(Map<String, Object> param, Object _id) {
        if (_id instanceof ObjectId) {
            param.put("_useObjectId", true);
            param.put("_id", _id.toString());
            return;
        }

        param.put("_useObjectId", false);
        param.put("_id", _id);
    }

    public static MongoCollectionResult findDocuments(MongoDatabaseNode database, String collection, MongoQueryOptions queryOptions) {
        String url = setUrlPath(Constants.Url.POST_MONGO_FIND_DOCUMENTS, "datasourceType", Constants.DataSource.MongoDB);
        Map<String, Object> param = new HashMap<>();
        param.put("databaseName", database.getName());
        param.put("collectionName", collection);
        param.put("queryOptionsStr", JSON.toJson(queryOptions));
        Map<String, Object> nodeAttr = database.getNodeAttr();
        if (MapUtils.isNotEmpty(nodeAttr)) {
            param.putAll(nodeAttr);
        }

        Result<MongoCollectionResult> result = catchException(url, () -> RestTemplateUtil.post(url, param, new HashMap<>(), MONGO_COLLECTION_RESULT), "查询MongoDocuments失败" + ADD_NOTIFY, false);
        return resultData(result, new MongoCollectionResult());
    }

    public static Document findDocument(MongoDatabaseNode database, String collection, Object _id) {
        String url = setUrlPath(Constants.Url.POST_MONGO_FIND_DOCUMENT, "datasourceType", Constants.DataSource.MongoDB);
        Map<String, Object> param = new HashMap<>();
        param.put("databaseName", database.getName());
        param.put("collectionName", collection);
        setObjectId(param, _id);
        Map<String, Object> nodeAttr = database.getNodeAttr();
        if (MapUtils.isNotEmpty(nodeAttr)) {
            param.putAll(nodeAttr);
        }

        Result<String> result = catchException(url, () -> RestTemplateUtil.post(url, param, new HashMap<>(), STRING), "查询MongoDocument失败" + ADD_NOTIFY, false);
        String docJson = resultData(result, "{}");
        return Document.parse(docJson);
    }

    public static boolean deleteDocument(MongoDatabaseNode database, String collection, Object _id) {
        String url = setUrlPath(Constants.Url.POST_MONGO_DELETE_DOCUMENT, "datasourceType", Constants.DataSource.MongoDB);
        Map<String, Object> param = new HashMap<>();
        param.put("databaseName", database.getName());
        param.put("collectionName", collection);
        setObjectId(param, _id);
        Map<String, Object> nodeAttr = database.getNodeAttr();
        if (MapUtils.isNotEmpty(nodeAttr)) {
            param.putAll(nodeAttr);
        }

        Result<String> result = catchException(url, () -> RestTemplateUtil.post(url, param, new HashMap<>(), STRING), "删除MongoDocument失败" + ADD_NOTIFY, false);
        return result.isSuccess();
    }

    public static boolean updateDocument(MongoDatabaseNode database, String collection, Document mongoDocument) {
        String url = setUrlPath(Constants.Url.POST_MONGO_UPDATE_DOCUMENT, "datasourceType", Constants.DataSource.MongoDB);
        Map<String, Object> param = new HashMap<>();
        param.put("databaseName", database.getName());
        param.put("collectionName", collection);
        param.put("mongoDocumentStr", mongoDocument.toJson());
        Map<String, Object> nodeAttr = database.getNodeAttr();
        if (MapUtils.isNotEmpty(nodeAttr)) {
            param.putAll(nodeAttr);
        }

        Result<Object> result = catchException(url, () -> RestTemplateUtil.post(url, param, new HashMap<>(), OBJECT), "更新MongoDocument失败" + ADD_NOTIFY, false);
        return result.isSuccess();
    }
}
