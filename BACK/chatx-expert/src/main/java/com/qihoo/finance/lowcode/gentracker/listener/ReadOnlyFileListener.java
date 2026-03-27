package com.qihoo.finance.lowcode.gentracker.listener;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.gentracker.config.SettingsSettingStorage;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.dto.GenerateFileCodeDTO;
import com.qihoo.finance.lowcode.gentracker.entity.Template;
import com.qihoo.finance.lowcode.gentracker.entity.TemplateGroup;
import com.qihoo.finance.lowcode.gentracker.service.SettingsStorageService;
import com.qihoo.finance.lowcode.gentracker.tool.FileUtils;
import com.qihoo.finance.lowcode.gentracker.tool.Md5Utils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 插件生成只读文件变更监听器
 *
 * @author fengjinfu-jk
 * date 2023/8/2
 * @version 1.0.0
 * @apiNote EditListener
 */
@Slf4j
public class ReadOnlyFileListener implements BulkFileListener {
    private static final Map<String, String> FILE_MD5 = new HashMap<>();
    private static final Map<String, String> PATHS = new HashMap<>();
    private static final Map<String, Long> LOCK = new HashMap<>();

    /**
     * 相同文件至少间隔 {@code   LOCK_TIME_MS} 才做计算
     */
    private static final long LOCK_TIME_MS = 5 * 1000;
    private static final String ROOT_GEN_PATH = Constants.Package.AUTOGEN;

    static {
        // 监听器命中的文件路径及类型
        PATHS.put("controller", ".java");
        PATHS.put("dao", ".java");
        PATHS.put("domain", ".java");
        PATHS.put("service", ".java");
        PATHS.put("mapper", ".xml");
    }

    @Override
    public void before(@NotNull List<? extends @NotNull VFileEvent> events) {
        BulkFileListener.super.before(events);
    }

    @SneakyThrows
    @Override
    public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        if (!GenerateListenerManager.isReadOnly()) {
            BulkFileListener.super.after(events);
            return;
        }

        for (VFileEvent event : events) {
            if (!(event instanceof VFileContentChangeEvent)) {
                continue;
            }

            if (!isGenerateFile(event)) {
                continue;
            }

            if (!this.acquireLock(event)) {
                continue;
            }

            new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() {
                    try {
                        return validateReadOnly(event);
                    } catch (Exception e) {
                        return false;
                    }
                }
            }.execute();
        }

        BulkFileListener.super.after(events);
    }

    private boolean validateReadOnly(VFileEvent event) throws IOException {
        // current file codeMd5
        VirtualFile virtualFile = event.getFile();
        if (Objects.isNull(virtualFile)) {
            return false;
        }

        // 无改动或实现类修改不做校验
        String currentMd5 = Md5Utils.md5Digest(FileUtils.readVirtualFile(event.getFile()));
        if (doNothing(currentMd5, event.getPath())) {
            return false;
        }

        // 最新云端同步版本存储的生成记录
        GenerateFileCodeDTO fileCode = LowCodeAppUtils.getEventFileNewestCode(virtualFile);
        if (Objects.isNull(fileCode)) {
            // 首次生成文件, 无验证
            return false;
        }

        if (allowModify(fileCode, event)) {
            return false;
        }

        // md5验证文件修改
        String codeMd5 = StringUtils.defaultIfEmpty(fileCode.getCodeMd5(), Md5Utils.md5Digest(fileCode.getCode()));
        if (currentMd5.equals(codeMd5)) {
            log.debug("file {} md5 validate success, continue", virtualFile.getName());
            return false;
        }

        // 文件恢复并重置为只读
        GenerateListenerManager.readOnlyStop();
        File localFile = new File(virtualFile.getPath());
        coverFile(localFile, fileCode.getCode());
        if (localFile.setReadOnly()) {
            virtualFile.refresh(true, true);
            GenerateListenerManager.readOnlyStart();
            log.debug("file {} md5 validate failure, overwrite", virtualFile.getName());

            // 通知
            NotifyUtils.notify(String.format("禁止修改平台生成代码文件%s, 已自动恢复代码", virtualFile.getName()), NotificationType.WARNING);
        }

        return true;
    }

    private void coverFile(File file, String text) {
        try {
            if (file.setWritable(true)) {
                FileOutputStream fileOutputStream = new FileOutputStream(file.getPath());
                fileOutputStream.write(text.getBytes(StandardCharsets.UTF_8));
                fileOutputStream.flush();
                fileOutputStream.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean acquireLock(VFileEvent event) {
        // 相同文件内不连续检测
        VirtualFile file = event.getFile();
        if (Objects.isNull(file)) {
            return false;
        }

        String path = file.getPath();
        long now = System.currentTimeMillis();
        Long lastTime = ObjectUtils.defaultIfNull(LOCK.putIfAbsent(path, now), now);
        if (now - lastTime > LOCK_TIME_MS || now == lastTime) {
            LOCK.put(path, now);
            return true;
        }

        return false;
    }

    private boolean isGenerateFile(VFileEvent event) {
        VirtualFile file = event.getFile();
        if (Objects.isNull(file)) {
            return false;
        }

        String path = event.getPath();
        if (!path.contains(ROOT_GEN_PATH)) {
            return false;
        }
        for (Map.Entry<String, String> paths : PATHS.entrySet()) {
            String dirPath = paths.getKey();
            String fileType = paths.getKey();
            if (path.contains(dirPath) && path.contains(fileType)) {
                return true;
            }
        }

        return false;
    }

    private boolean allowModify(GenerateFileCodeDTO fileCode, VFileEvent event) {
        SettingsSettingStorage settings = SettingsStorageService.getSettingsStorage();
        String groupName = fileCode.getGroupName();
        String templateName = fileCode.getTemplateName();
        templateName = templateName.split("\\.")[0];

        TemplateGroup templateGroup = settings.getTemplateGroupMap().getOrDefault(StringUtils.defaultString(groupName, GlobalDict.DEFAULT_GROUP_NAME), new TemplateGroup());
        List<Template> elementList = ListUtils.defaultIfNull(templateGroup.getElementList(), new ArrayList<>());

        boolean canModify = false;
        for (Template template : elementList) {
            String name = template.getName().split("\\.")[0];
            if (name.equals(templateName)) {
                canModify = template.isCanModify();
                break;
            }
        }

        return canModify;
    }

    private boolean doNothing(String currentMd5, String path) {
        if (FILE_MD5.containsKey(path)) {
            String cacheMd5 = FILE_MD5.get(path);
            // 如果与上一次一致则无需进行额外请求判断
            return currentMd5.equals(cacheMd5);
        }

        FILE_MD5.put(path, currentMd5);
        return false;
    }
}
