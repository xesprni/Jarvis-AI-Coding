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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.bson.Document;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class MongoCollectionResult {
    private String collectionName;
    private long countDocuments;
    private int totalDocumentNumber;
    private List<String> docJsons;
    @JsonIgnore
    private List<Document> documents;

    public void setDocJsons(List<String> docJsons) {
        this.docJsons = docJsons;
        this.documents = this.docJsons.stream().map(Document::parse).collect(Collectors.toList());
    }
}
