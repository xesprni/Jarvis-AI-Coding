package com.qihoo.finance.lowcode.console.mongo;

import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.console.mongo.view.editor.MongoFileSystem;
import com.qihoo.finance.lowcode.console.mongo.view.editor.MongoVirtualFile;
import com.qihoo.finance.lowcode.design.entity.MongoCollectionNode;
import org.jetbrains.annotations.NotNull;

/**
 * MongoConsoleManager
 *
 * @author fengjinfu-jk
 * date 2024/1/19
 * @version 1.0.0
 * @apiNote MongoConsoleManager
 */
public class MongoEditorManager {
    private final Project project;

    public MongoEditorManager(@NotNull Project project) {
        this.project = project;
    }

    public void openMongoEditor(MongoCollectionNode mongoCollection) {
        MongoFileSystem.getInstance().openEditor(new MongoVirtualFile(project, mongoCollection));
    }
}
