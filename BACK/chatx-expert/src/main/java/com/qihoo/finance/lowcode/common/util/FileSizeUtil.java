package com.qihoo.finance.lowcode.common.util;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FileSizeUtil {

    public static final long MAX_FILE_SIZE = 1048576L;

    public static boolean isSupported(@Nullable VirtualFile file) {
        return file != null && file.getLength() <= 1048576L;
    }

    public static boolean isTooLarge(@NotNull VirtualFile file) {
        return file.getLength() > 1048576L;
    }
}
