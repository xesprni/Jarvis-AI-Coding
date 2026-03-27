package com.qihoo.finance.lowcode.gentracker.tool;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.qihoo.finance.lowcode.common.util.ApplicationUtil;
import org.apache.commons.lang3.ObjectUtils;

/**
 * IDEA项目相关工具
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public class ProjectUtils {

    public static String getCurrProjectName() {
        return getCurrProject().getName();
    }

    /**
     * 获取当前项目对象
     *
     * @return 当前项目对象
     */
    public static Project getCurrProject() {
        try {
            ProjectManager projectManager = ProjectManager.getInstance();
            Project currentProject = ApplicationUtil.findCurrentProject();

            //否则使用默认项目
            return ObjectUtils.defaultIfNull(currentProject, projectManager.getDefaultProject());
        } catch (Exception e) {
            return ProjectManager.getInstance().getDefaultProject();
        }
    }

    /**
     * 进行旧版本兼容，该方法已经存在 @see {@link com.intellij.openapi.project.ProjectUtil#guessProjectDir(Project)}
     *
     * @param project 项目对象
     * @return 基本目录
     */
    public static VirtualFile getBaseDir(Project project) {
        if (project.isDefault()) {
            return null;
        }
        Module[] modules = ModuleManager.getInstance(project).getModules();
        Module module = null;
        if (modules.length == 1) {
            module = modules[0];
        } else {
            for (Module item : modules) {
                if (item.getName().equals(project.getName())) {
                    module = item;
                    break;
                }
            }
        }
        if (module != null) {
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
            for (VirtualFile contentRoot : moduleRootManager.getContentRoots()) {
                if (contentRoot.isDirectory() && contentRoot.getName().equals(module.getName())) {
                    return contentRoot;
                }
            }
        }
        String basePath = project.getBasePath();
        if (basePath == null) {
            throw new NullPointerException();
        }
        return LocalFileSystem.getInstance().findFileByPath(basePath);
    }
}
