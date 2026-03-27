package com.qihoo.finance.lowcode.gentracker.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * IoUtils
 *
 * @author fengjinfu-jk
 * date 2023/11/30
 * @version 1.0.0
 * @apiNote IoUtils
 */
@Slf4j
public class IoUtils {
    @SneakyThrows
    public static void writeFile(String filePath, String content, boolean refresh) {
        int split = filePath.lastIndexOf("/");

        String dirPath = filePath.substring(0, split);
        File dir = new File(dirPath);
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("mkdirs error: " + dirPath);

        File file = new File(filePath);
        if (!file.exists() && !file.createNewFile()) throw new IOException("createNewFile error: " + filePath);

        writeFile(file, content, refresh);
    }

    public static void writeFile(File file, String content, boolean refresh) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            bufferedWriter.write(content);
            // 添加换行符
            bufferedWriter.newLine();

            log.info("数据已成功写入文件：{}", file.getName());
        } catch (IOException e) {
            log.error("写入文件时出现错误，{} : {}", file.getName(), e.getMessage());
        }

        if (refresh) {
            ApplicationManager.getApplication().invokeLater(() -> {
                try {
                    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file.getCanonicalFile());
                } catch (IOException e) {
                    // ignore
                }
            });
        }
    }
}
