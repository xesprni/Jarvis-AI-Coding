package com.qihoo.finance.lowcode.gentracker.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qihoo.finance.lowcode.design.entity.MongoCollectionNode;
import lombok.Data;

/**
 * MongoCollection
 *
 * @author fengjinfu-jk
 * date 2023/12/25
 * @version 1.0.0
 * @apiNote MongoCollection
 */
@Data
public class MongoCollection {
    /**
     * 原始对象
     */
    @JsonIgnore
    private MongoCollectionNode obj;
    private String json;
    private String jsonRaw;
    private String templateGroup;
    private String moduleName;
    private String projectName;

    public MongoCollection(MongoCollectionNode obj) {
        this.obj = obj;
    }
}
