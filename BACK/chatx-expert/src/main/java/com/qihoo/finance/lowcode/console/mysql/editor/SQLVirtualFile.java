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

package com.qihoo.finance.lowcode.console.mysql.editor;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.LocalTimeCounter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;

public class SQLVirtualFile extends LightVirtualFile {

    private final Project project;
    private final String path;

    public SQLVirtualFile(Project project, String fileName, FileType fileType) {
        super(fileName, fileType, "");
        this.project = project;
        this.path = fileName;
    }

    @NotNull
    @Override
    public String getName() {
        return path;
    }

    @NotNull
    public FileType getFileType() {
        return SQLFakeFileType.INSTANCE;
    }

    @NotNull
    @Override
    public VirtualFileSystem getFileSystem() {
        return SQLFileSystem.getInstance();
    }

    @NotNull
    @Override
    public String getPath() {
        return path;
    }

    public Project getProject() {
        return project;
    }

    @NotNull
    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) {
        throw new UnsupportedOperationException("MySQLResultFile is read-only");
    }
}
