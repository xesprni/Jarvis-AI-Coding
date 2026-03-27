package com.qihoo.finance.lowcode.gentracker.entity;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.gentracker.dto.GenerateVelocityOptions;
import com.qihoo.finance.lowcode.gentracker.tool.CompareFileUtils;
import com.qihoo.finance.lowcode.gentracker.tool.FileUtils;
import com.qihoo.finance.lowcode.gentracker.tool.MessageDialogUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 需要保存的文件
 * <p>
 * 如果文件保存在项目路径下，则使用idea提供的psi对象操作。如果文件保存在非项目路径下，则使用java原始IO流操作。
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
@Data
@Slf4j
public class SaveFile {
    private Template template;
    /**
     * 所属项目
     */
    private Project project;
    /**
     * 文件内容
     */
    private String content;
    /**
     * 文件工具类
     */
    private FileUtils fileUtils = FileUtils.getInstance();
    /**
     * 回调对象
     */
    private Callback callback;
    private Boolean isModify;
    /**
     * 生成配置
     */
    private GenerateVelocityOptions generateOptions;
    private LocalFileSystem fileSystem = LocalFileSystem.getInstance();
    private boolean ignore;

    /**
     * 保存文件
     *
     * @param project         项目
     * @param content         内容
     * @param callback        回调
     * @param generateOptions 生成选项
     */
    public SaveFile(@NonNull Project project, @NonNull String content, @NonNull Callback callback, @NonNull GenerateVelocityOptions generateOptions) {
        this.project = project;
        this.callback = callback;
        this.content = content.replace("\r", "");
        this.generateOptions = generateOptions;
    }

    public boolean isModify() {
        if (Objects.isNull(isModify)) {
            SaveFile.VirtualFileResult virtualFile = this.getVirtualFile(false);
            isModify = virtualFile != null && virtualFile.getVirtualFile() != null && virtualFile.getVirtualFile().exists();
        }

        return isModify;
    }

    /**
     * 文件是否为项目文件
     *
     * @return 是否为项目文件
     */
    private boolean isProjectFile() {
        VirtualFile baseDir = ProjectUtils.getBaseDir(project);
        // 无法获取到项目基本目录，可能是Default项目，直接返回非项目文件
        if (baseDir == null) {
            return false;
        }
        // 路径对比，判断项目路径是否为文件保存路径的子路径
        String projectPath = handlerPath(baseDir.getPath());
        String tmpFilePath = handlerPath(callback.getSavePath());
        if (tmpFilePath.length() > projectPath.length()) {
            if (!"/".equals(tmpFilePath.substring(projectPath.length(), projectPath.length() + 1))) {
                return false;
            }
        }
        return tmpFilePath.indexOf(projectPath) == 0;
    }

    /**
     * 处理路径，统一分割符并转小写
     *
     * @param path 路径
     * @return 处理后的路径
     */
    private String handlerPath(String path) {
        return handlerPath(path, true);
    }

    /**
     * 处理路径，统一分割符并转小写
     *
     * @param path      路径
     * @param lowerCase 是否转小写
     * @return 处理后的路径
     */
    private String handlerPath(String path, boolean lowerCase) {
        // 统一分割符
        path = path.replace("\\", "/");
        // 避免重复分割符
        path = path.replace("//", "/");
        // 统一小写
        return lowerCase ? path.toLowerCase() : path;
    }

    /**
     * 通过IDEA自带的Psi文件方式写入
     */
    public Pair<VirtualFile, String> write(boolean forcedOverlay, SaveOptions saveOptions) {
        VirtualFileResult fileResult = getVirtualFile();
        if (Objects.isNull(fileResult)) return null;

        // 保存或覆盖
        return saveOrReplaceFile(fileResult.getVirtualFile(), fileResult.getDirectory(), forcedOverlay, saveOptions);
    }

    public VirtualFileResult getVirtualFile() {
        return getVirtualFile(true);
    }

    public VirtualFileResult getVirtualFile(boolean createDirIfNotExist) {
        if (!Boolean.TRUE.equals(callback.getWriteFile())) {
            return null;
        }
        // 判断目录是否存在
        VirtualFile baseDir = ProjectUtils.getBaseDir(project);
        if (baseDir == null) {
            throw new IllegalStateException("项目基本路径不存在");
        }
        // 处理保存路径
        String savePath = handlerPath(callback.getSavePath(), false);
        if (isProjectFile()) {
            // 删除保存路径的前面部分
            savePath = savePath.substring(handlerPath(baseDir.getPath()).length());
        } else {
            baseDir = null;
        }
        // 删除开头与结尾的/符号
        while (savePath.startsWith("/")) {
            savePath = savePath.substring(1);
        }
        while (savePath.endsWith("/")) {
            savePath = savePath.substring(0, savePath.length() - 1);
        }
        // 查找保存目录是否存在
        VirtualFile saveDir;
        if (baseDir == null) {
            saveDir = VfsUtil.findFileByIoFile(new File(savePath), false);
        } else {
            saveDir = VfsUtil.findRelativeFile(baseDir, savePath.split("/"));
        }
        if (null == saveDir && !createDirIfNotExist) return null;

        // 提示创建目录
        VirtualFile directory = titleCreateDir(saveDir, baseDir, savePath);
        if (directory == null) {
            return null;
        }

        VirtualFile psiFile = directory.findChild(callback.getFileName());
        VirtualFileResult result = new VirtualFileResult();
        result.setVirtualFile(psiFile);
        result.setDirectory(directory);

        return result;
    }

    /**
     * 提示创建目录
     *
     * @param saveDir 保存路径
     * @return 是否放弃执行
     */
    private VirtualFile titleCreateDir(VirtualFile saveDir, VirtualFile baseDir, String savePath) {
        if (saveDir != null) {
            return saveDir;
        }
        // 尝试创建目录
//        String msg = String.format("Directory %s Not Found, Confirm Create?", callback.getSavePath());
        String msg = String.format("文件路径 %s 不存在, 是否创建?", callback.getSavePath());
        if (generateOptions.getTitleSure()) {
            saveDir = fileUtils.createChildDirectory(project, baseDir, savePath);
            return saveDir;
        } else if (generateOptions.getTitleRefuse()) {
            return null;
        } else {
            if (MessageDialogUtils.yesNo(project, msg)) {
                saveDir = fileUtils.createChildDirectory(project, baseDir, savePath);
                return saveDir;
            }
        }
        return null;
    }

    /**
     * 保存或替换文件
     *
     * @param virtualFile 文件
     * @param directory   目录
     * @param overlay     强制覆盖
     * @param saveOptions
     */
    private Pair<VirtualFile, String> saveOrReplaceFile(VirtualFile virtualFile, VirtualFile directory, boolean overlay, SaveOptions saveOptions) {
        // 文件不存在直接创建
        File file;
        if (virtualFile == null) {
            virtualFile = fileUtils.createChildFile(project, directory, callback.getFileName());
            if (virtualFile == null) {
                return null;
            }

            file = new File(virtualFile.getPath());
            coverFile(file);
        } else {
            // 打开文件权限
            file = new File(virtualFile.getPath());
            boolean writable = file.setWritable(true);
            if (writable) {
                log.debug("writable file: {}", file.getPath());
            }

            // 覆盖文件
            if (overlay) {
                // 默认选是
                coverFile(file);
            }  // 暂不处理非强制覆盖的文件

        }

        virtualFile.refresh(true, true);
        return Pair.of(virtualFile, content);
    }

    private void showFileDiff(File file, SaveOptions saveOptions) {
        if (saveOptions.isOverwriteAll()) {
            coverFile(file);
            return;
        }

        String msg = String.format("文件 %s 已存在, 请选择处理方式?", file.getName());
        String[] options = new String[]{"全部覆盖", "覆盖", "保留原文件", "比对差异"};
        int diff = 3;
        int select = Messages.showDialog(msg, "文件生成", options, diff, Icons.scaleToWidth(Icons.WARNING, 50));

        if (select == 0) {
            // 全部覆盖
            coverFile(file);
            saveOptions.setOverwriteAll(true);
        } else if (select == 1) {
            // 覆盖当前文件
            coverFile(file);
        } else if (select == 2) {
            // 保留原文件;
            // do nothing
        } else if (select == diff) {
            // 比对差异
            showDiffWindows(file);
        }
    }

    public void showDiffWindows(File file) {
        String newText = content;
        FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(callback.getFileName());
        VirtualFile orgFile = ObjectUtils.defaultIfNull(fileSystem.findFileByIoFile(file), fileSystem.refreshAndFindFileByIoFile(file));
        LightVirtualFile newContent = new LightVirtualFile("新生成代码(" + callback.getFileName() + ")", fileType, newText);
        CompareFileUtils.showCompareWindow(project, newContent, orgFile);
    }

    public void showDiffWindows() {
        String newText = content;
        FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(callback.getFileName());
        LightVirtualFile newContent = new LightVirtualFile("新生成代码(" + callback.getFileName() + ")", fileType, newText);
        VirtualFileResult virtualFileResult = getVirtualFile();
        
        VirtualFile orgFile;
        if (virtualFileResult == null || virtualFileResult.getVirtualFile() == null) {
            orgFile = new LightVirtualFile("", fileType, "");
        } else {
            VirtualFile virtualFile = virtualFileResult.getVirtualFile();
            File file = new File(virtualFile.getPath());
            orgFile = ObjectUtils.defaultIfNull(fileSystem.findFileByIoFile(file), fileSystem.refreshAndFindFileByIoFile(file));
        }
        
        CompareFileUtils.showCompareWindowWithCallback(project, orgFile, newContent, (modifiedContent) -> {
            this.content = modifiedContent;
        });
    }

    private String getFileText(VirtualFile file) {
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        Document document = fileDocumentManager.getDocument(file);
        if (document == null) {
            throw new IllegalStateException("virtual file to document failure");
        }
        return document.getText();
    }

    private String getFileText(File file) {
        try {
            return FileUtils.readFileInputStream(new FileInputStream(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 覆盖文件
     *
     * @param file 文件
     * @return 覆盖后的文档对象
     */
    private void coverFile(File file) {
        coverFile(file, content);
    }

    /**
     * 覆盖文件
     *
     * @param file 文件
     * @return 覆盖后的文档对象
     */
    private Document coverFile(VirtualFile file) {
        return coverFile(file, content);
    }

    /**
     * 覆盖文件
     *
     * @param text 文件内容
     * @return 覆盖后的文档对象
     */
    private Document coverFile(VirtualFile virtualFile, String text) {
        return FileUtils.getInstance().writeFileContent(project, virtualFile, callback.getFileName(), text);
    }

    /**
     * 覆盖文件
     *
     * @param text 文件内容
     * @return 覆盖后的文档对象
     */
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

    @Data
    public static class SaveOptions {
        private boolean overwriteAll = false;
    }

    @Data
    public static class VirtualFileResult {
        private VirtualFile directory;
        private VirtualFile virtualFile;
    }
}
