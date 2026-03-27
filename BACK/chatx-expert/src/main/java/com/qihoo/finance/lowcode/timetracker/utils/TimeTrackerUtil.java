package com.qihoo.finance.lowcode.timetracker.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.timetrack.ReadWritePushDTO;
import com.qihoo.finance.lowcode.common.entity.timetrack.ReadWritePushResponse;
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;

/**
 * Utility methods.
 *
 * @author weiyichao
 */
public final class TimeTrackerUtil extends LowCodeAppUtils {

    /**
     * Rounded conversion of milliseconds to seconds.
     */
    public static long msToS(long ms) {
        return (ms) / 1000L;
    }

    @Nullable
    public static VirtualFile getProjectBaseDir(@Nullable Project project) {
        if (project == null) {
            return null;
        }
        final String basePath = PathUtil.toSystemIndependentName(project.getBasePath());
        if (basePath == null) {
            return null;
        }
        return LocalFileSystem.getInstance().findFileByPath(basePath);
    }

    @Nullable
    public static Path convertToIOFile(@Nullable VirtualFile file) {
        if (file == null || !file.isInLocalFileSystem()) {
            return null;
        }

        // Based on LocalFileSystemBase.java
        String path = file.getPath();
        if (StringUtil.endsWithChar(path, ':') && path.length() == 2 && SystemInfo.isWindows) {
            path += "/";
        }

        return Paths.get(path);
    }

    //------------------------------------------------------------------------------------------------------------------

    private static final TypeReference<Result<ReadWritePushResponse>> RW_PUSH_RESP = new TypeReference<>() {
    };

    public static ReadWritePushResponse batchPushTimeTrack(ArrayDeque<ReadWritePushDTO> readWritePushList) {
        String url = Constants.Url.POST_READ_WRITE_COUNT;
        Result<ReadWritePushResponse> result = catchException(url, () ->
                RestTemplateUtil.post(url, readWritePushList, APPLICATION_JSON_HEADERS, RW_PUSH_RESP), "读写耗时上报" + ADD_NOTIFY, false);
        return resultData(result);
    }
}
