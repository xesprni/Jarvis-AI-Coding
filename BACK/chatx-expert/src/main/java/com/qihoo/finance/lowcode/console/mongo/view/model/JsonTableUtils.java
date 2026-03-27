/*
 * Copyright (c) 2018 David Boissier.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qihoo.finance.lowcode.console.mongo.view.model;

import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.qihoo.finance.lowcode.console.mongo.view.renderer.MongoTableCellRenderer;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import javax.swing.table.TableCellRenderer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonTableUtils {


    public static ListTableModel buildJsonTable(MongoCollectionResult mongoCollectionResult) {
        try {
            List<Document> resultObjects = mongoCollectionResult.getDocuments();
//            if (resultObjects.isEmpty()) {
//                return new ListTableModel<>(extractColumnNames(new Document()), resultObjects);
//            }
            ColumnInfo[] columnInfos = extractColumnNames(ObjectUtils.defaultIfNull(resultObjects, new ArrayList<>()));
            return new ListTableModel<>(columnInfos, resultObjects);
        } catch (Exception e) {
            return new ListTableModel<>(new ColumnInfo[]{}, new ArrayList<>());
        }
    }

    public static ColumnInfo[] extractColumnNames(final Document document) {
        try {
            Set<String> keys = document.keySet();
            ColumnInfo[] columnInfos = new ColumnInfo[keys.size()];
            int index = 0;
            for (final String key : keys) {
                columnInfos[index++] = new TableColumnInfo(key);
            }
            return columnInfos;
        } catch (Exception e) {
            return new ColumnInfo[]{};
        }
    }

    public static ColumnInfo[] extractColumnNames(final List<Document> documents) {
        try {
            // 总字段列表
            List<String> allKeys = documents.stream().flatMap(doc -> doc.keySet().stream()).toList();
            LinkedHashSet<String> keys = new LinkedHashSet<>(allKeys);
            // 首行字段列表
            ColumnInfo[] columnInfos = new ColumnInfo[keys.size()];
            int index = 0;
            for (final String key : keys) {
                columnInfos[index++] = new TableColumnInfo(key);
            }
            return columnInfos;
        } catch (Exception e) {
            return new ColumnInfo[]{};
        }
    }

    private static void extractNestColumnNames(List<String> allKeys, String parentKey, final Document document) {
        Set<String> keys = document.keySet();
        for (String key : keys) {
            Object value = document.get(key);
            if (value instanceof Document) {
                String nestKey = StringUtils.isEmpty(parentKey) ? key : parentKey + "." + key;
                extractNestColumnNames(allKeys, nestKey, (Document) value);
            }
        }
        if (StringUtils.isNotEmpty(parentKey)) {
            allKeys.addAll(keys.stream().map(k -> parentKey + "." + k).collect(Collectors.toList()));
            return;
        }

        allKeys.addAll(keys);
    }

    public static List<String> extractAllColumnNames(final Document document) {
        List<String> fields = new ArrayList<>();
        extractNestColumnNames(fields, null, document);
        return fields;
    }

    private static class TableColumnInfo extends ColumnInfo {
        private final String key;

        private static final TableCellRenderer MONGO_TABLE_CELL_RENDERER = new MongoTableCellRenderer();

        TableColumnInfo(String key) {
            super(key);
            this.key = key;
        }

        @Nullable
        @Override
        public Object valueOf(Object o) {
            Document document = (Document) o;
            return document.get(key);
        }

        @Nullable
        @Override
        public TableCellRenderer getRenderer(Object o) {
            return MONGO_TABLE_CELL_RENDERER;
        }
    }
}
