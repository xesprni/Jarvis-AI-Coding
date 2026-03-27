package com.qihoo.finance.lowcode.console.mongo.view.editor;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.console.completion.CompletionKeywords;
import com.qihoo.finance.lowcode.console.completion.CompletionProvider;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * MongoCompletionProvider
 *
 * @author fengjinfu-jk
 * date 2024/1/25
 * @version 1.0.0
 * @apiNote MongoCompletionProvider
 */
public class MongoCompletionProvider extends CompletionProvider {
    private final MongoVirtualFile virtualFile;

    public MongoCompletionProvider(MongoVirtualFile virtualFile) {
        this.virtualFile = virtualFile;
    }

    @Override
    protected void addCompletionVariants(@NotNull String text, int offset, @NotNull String prefix
            , @NotNull CompletionResultSet result) {
        result.addAllElements(CompletionKeywords.MongoLookup);

        // schema
        String database = virtualFile.getMongoCollection().getDatabase();
        result.addElement(LookupElementBuilder.create(database).withPresentableText(database)
                .withIcon(Icons.scaleToWidth(Icons.DB_BLOCK, 18)).withTypeText("schema").bold());
        // tableName
        String tableName = virtualFile.getMongoCollection().getTableName();
        result.addElement(LookupElementBuilder.create(tableName).withPresentableText(tableName)
                .withIcon(Icons.scaleToWidth(Icons.TABLE2, 16)).withTypeText("table").bold());
        result.addElement(LookupElementBuilder.create(database + "." + tableName).withPresentableText(database + "." + tableName)
                .withIcon(Icons.scaleToWidth(Icons.TABLE2, 16)).withTypeText("table").bold());
        // fields
        List<String> fieldList = virtualFile.getUserData(MongoVirtualFile.COLLECTION_FIELD_LIST);
        if (CollectionUtils.isNotEmpty(fieldList)) {
            for (String field : fieldList) {
                result.addElement(LookupElementBuilder.create(field).withPresentableText(field)
                        .withIcon(Icons.scaleToWidth(Icons.COLUMN, 16)).withTypeText("field").bold());
                result.addElement(LookupElementBuilder.create(tableName + "." + field).withPresentableText(tableName + "." + field)
                        .withIcon(Icons.scaleToWidth(Icons.COLUMN, 16)).withTypeText("field").bold());
            }

        }
    }
}
