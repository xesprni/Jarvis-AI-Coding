package com.qihoo.finance.lowcode.gentracker.service.impl;

import com.intellij.ide.actions.OpenFileAction;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileChooser.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.util.ExceptionUtil;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.config.SettingsSettingStorage;
import com.qihoo.finance.lowcode.gentracker.service.ExportImportSettingsService;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 本地文件导入导出设置服务实现
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public class LocalFileExportImportSettingsServiceImpl implements ExportImportSettingsService {
    /**
     * 导出设置
     *
     * @param settingsStorage 要导出的设置
     */
    @Override
    public void exportConfig(SettingsSettingStorage settingsStorage) {
        // 1.选择储存位置
        FileSaverDialog saveFileDialog = FileChooserFactory.getInstance().createSaveFileDialog(new FileSaverDescriptor("Save Config As Json", "Save to"), ProjectUtils.getCurrProject());
        VirtualFileWrapper saveFile = saveFileDialog.save((VirtualFile) null, "QiMaExpertsGenerateConfig.json");
        if (saveFile == null) {
            return;
        }
        File file = saveFile.getFile();
        // 2.执行导出
        FileUtil.createIfDoesntExist(file);
        WriteCommandAction.runWriteCommandAction(ProjectUtils.getCurrProject(), () -> {
            try {
                byte[] bytes = JSON.toJsonByFormat(settingsStorage).getBytes(StandardCharsets.UTF_8);
                VirtualFile virtualFile = VfsUtil.findFileByIoFile(file, true);
                if (virtualFile != null) {
                    virtualFile.setBinaryContent(bytes);
                    FileDocumentManager.getInstance().reloadFiles(virtualFile);
                }

                // 发起通知
                Notification notification = new Notification(
                        "System Messages",
                        GlobalDict.PLUGIN_NAME + " notify",
                        GlobalDict.PLUGIN_NAME + " config file export to",
                        NotificationType.INFORMATION);
                notification.addAction(new AnAction(file.getName()) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        // 打开文件
                        if (virtualFile != null) {
                            OpenFileAction.openFile(virtualFile, ProjectUtils.getCurrProject());
                        }
                    }
                });
                Notifications.Bus.notify(notification, ProjectUtils.getCurrProject());
            } catch (IOException e) {
                ExceptionUtil.rethrow(e);
            }
        });
    }

    /**
     * 导入设置
     *
     * @return 设置信息
     */
    @Override
    public SettingsSettingStorage importConfig() {
        VirtualFile virtualFile = FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFileDescriptor("json"), ProjectUtils.getCurrProject(), null);
        if (virtualFile == null) {
            Messages.showWarningDialog("Config file not found !", GlobalDict.TITLE_INFO);
            return null;
        }
        String json = LoadTextUtil.loadText(virtualFile).toString();
        return JSON.parse(json, SettingsSettingStorage.class);
    }
}
