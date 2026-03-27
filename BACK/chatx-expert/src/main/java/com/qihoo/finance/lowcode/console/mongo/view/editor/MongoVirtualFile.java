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

package com.qihoo.finance.lowcode.console.mongo.view.editor;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.util.LocalTimeCounter;
import com.qihoo.finance.lowcode.design.entity.MongoCollectionNode;
import com.qihoo.finance.lowcode.console.mongo.model.MongoQueryOptions;
import com.qihoo.finance.lowcode.console.mongo.view.model.navigation.Navigation;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MongoVirtualFile extends VirtualFile {
    public static final Key<List<String>> COLLECTION_FIELD_LIST = Key.create("COLLECTION_FIELD_LIST");
    private final long myModStamp;
    private final Project project;
    private final String path;
    private final Navigation navigation;
    @Getter
    private final MongoCollectionNode mongoCollection;

    public MongoVirtualFile(Project project, MongoCollectionNode mongoCollection) {
        this.project = project;
        this.mongoCollection = mongoCollection;
        this.myModStamp = LocalTimeCounter.currentTime();
        this.path = getName();

        this.navigation = new Navigation();
        MongoQueryOptions queryOptions = new MongoQueryOptions();
        queryOptions.setResultLimit(50);
        navigation.addNewWayPoint(mongoCollection, queryOptions);
    }

    public Navigation getNavigation() {
        return navigation;
    }

    @NotNull
    @Override
    public String getName() {
        return String.format("MongoDB %s", mongoCollection.getTableName());
    }

    @NotNull
    public FileType getFileType() {
        return MongoFakeFileType.INSTANCE;
    }

    @NotNull
    @Override
    public VirtualFileSystem getFileSystem() {
        return MongoFileSystem.getInstance();
    }

    @NotNull
    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    public Project getProject() {
        return project;
    }

    //    Unused methods
    @Override
    public VirtualFile getParent() {
        return null;
    }

    @Override
    public VirtualFile[] getChildren() {
        return new VirtualFile[0];
    }

    @NotNull
    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) {
        throw new UnsupportedOperationException("MongoResultFile is read-only");
    }

    @Override
    public long getModificationStamp() {
        return myModStamp;
    }

    @NotNull
    @Override
    public byte[] contentsToByteArray() {
        return new byte[0];
    }

    @Override
    public long getTimeStamp() {
        return 0;
    }

    @Override
    public long getLength() {
        return 0;
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {

    }

    @Override
    public InputStream getInputStream() {
        return null;
    }
}
