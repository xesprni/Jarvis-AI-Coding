package com.qihoo.finance.lowcode.gentracker.service;

import com.intellij.openapi.application.ApplicationManager;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.entity.SaveFile;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.gentracker.ui.dialog.FileDiffDialog;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 代码生成服务，Project级别Service
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public interface BaseGenerateService {
    default String getUserEmail() {
        UserInfoPersistentState userInfoPersistentState = ApplicationManager.getApplication().getService(UserInfoPersistentState.class);
        UserInfoPersistentState.UserInfo user = userInfoPersistentState.getState();
        return Objects.nonNull(user) ? user.email : GlobalDict.AUTHOR;
    }

    default String getUserNo() {
        return getUserEmail().split("@")[0];
    }

    default List<SaveFile> executeSaveFile(List<SaveFile> saveFiles) {
        new FileDiffDialog(ProjectUtils.getCurrProject(), saveFiles).showAndGet();
        return saveFiles.stream().filter(s -> !s.isIgnore()).collect(Collectors.toList());
    }

    default void setReadOnly(String path) {
        File file = new File(path);
        if (file.isFile()) {
            file.setReadOnly();
        }
    }
}
