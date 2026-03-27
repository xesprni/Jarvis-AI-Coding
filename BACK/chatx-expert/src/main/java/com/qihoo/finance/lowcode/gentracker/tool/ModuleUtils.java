package com.qihoo.finance.lowcode.gentracker.tool;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.ProjectViewImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

/**
 * 模块工具类
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public final class ModuleUtils {
    /**
     * 禁用构造方法
     */
    private ModuleUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * 获取module路径
     *
     * @param module 模块
     * @return 路径
     */
    public static VirtualFile getModuleDir(@NotNull Module module) {
        // 优先使用ModuleRootManager来获取module路径
        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
        for (VirtualFile contentRoot : moduleRootManager.getContentRoots()) {
            if (contentRoot.isDirectory() && contentRoot.getName().equals(module.getName())) {
                return contentRoot;
            }
        }
        String modulePath = ModuleUtil.getModuleDirPath(module);
        // 统一路径分割符号
        modulePath = modulePath.replace("\\", "/");
        // 尝试消除不正确的路径
        if (modulePath.contains(".idea/modules/")) {
            modulePath = modulePath.replace(".idea/modules/", "");
        }
        if (modulePath.contains(".idea/modules")) {
            modulePath = modulePath.replace(".idea/modules", "");
        }
        if (modulePath.contains("/.idea")) {
            modulePath = modulePath.replace("/.idea", "");
        }
        VirtualFile dir = VirtualFileManager.getInstance().findFileByUrl(String.format("file://%s", modulePath));
        if (dir == null) {
            Messages.showInfoMessage("无法获取Module路径, path=" + modulePath, GlobalDict.TITLE_INFO);
        }
        return dir;
    }

    /**
     * 获取模块的源代码文件夹，不存在
     *
     * @param module 模块对象
     * @return 文件夹路径
     */
    public static VirtualFile getSourcePath(@NotNull Module module) {
        return getSourcePath(module, JavaSourceRootType.SOURCE);
    }

    public static VirtualFile getSourcePath(@NotNull Module module, JpsModuleSourceRootType<?> rootType) {
        List<VirtualFile> virtualFileList = ModuleRootManager.getInstance(module).getSourceRoots(rootType);
        if (CollectionUtil.isEmpty(virtualFileList)) {
            VirtualFile modulePath = getModuleDir(module);
            // 尝试智能识别源代码路径(通过上面的方式，IDEA不能百分百拿到源代码路径)
            VirtualFile srcDir;
            if (JavaSourceRootType.SOURCE.equals(rootType)) {
                srcDir = VfsUtil.findRelativeFile(modulePath, "src", "main", "java");
            } else {
                srcDir = VfsUtil.findRelativeFile(modulePath, "src", "main", "resources");
            }

            if (srcDir != null && srcDir.isDirectory()) {
                return srcDir;
            }
            return modulePath;
        }
        if (virtualFileList.size() > 1) {
            for (VirtualFile file : virtualFileList) {
                String tmpPath = file.getPath();
                if (!tmpPath.contains("build") && !tmpPath.contains("generated")) {
                    return file;
                }
            }
        }
        return virtualFileList.get(0);
    }

    public static PsiFile findPsiFileByDir(PsiDirectory psiDirectory, String fileName) {
        PsiFile psiFile = psiDirectory.findFile(fileName);
        if (psiFile != null) {
            return psiFile;
        }
        PsiDirectory[] psiDirectories = psiDirectory.getSubdirectories();
        for (PsiDirectory directory : psiDirectories) {
            PsiFile tempPsiFile = findPsiFileByDir(directory, fileName);
            if (tempPsiFile != null) {
                return tempPsiFile;
            }
        }
        return null;
    }

    /**
     * 判断模块是否存在源代码文件夹
     *
     * @param module 模块对象
     * @return 是否存在
     */
    public static boolean existsSourcePath(Module module) {
        return !CollectionUtil.isEmpty(ModuleRootManager.getInstance(module).getSourceRoots(JavaSourceRootType.SOURCE));
    }

    public static void selectAndOpenPackage(Project project, Module module, String packagePath) {
        selectAndOpenPackage(project, module, packagePath, true);
    }

    public static void selectAndOpenPackage(Project project, Module module, String packagePath, boolean srcPackage) {
        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
        if (contentRoots.length == 0) {
            return;
        }

        VirtualFile contentRoot = contentRoots[0];
        PsiDirectory targetDirectory = PsiManager.getInstance(project).findDirectory(contentRoot);
        if (targetDirectory != null) {
            String path = packagePath;
            if (srcPackage) path = "src.main.java." + packagePath;
            PsiDirectory directory = findSubDirectory(targetDirectory, path);
            ProjectView projectView = ProjectView.getInstance(project);
            if (projectView instanceof ProjectViewImpl) {
                ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                toolWindowManager.invokeLater(() -> {
                    // 选中
                    ProjectViewImpl projectViewImpl = (ProjectViewImpl) projectView;
                    projectViewImpl.select(directory, directory.getVirtualFile(), true);

                    // 展开
                    // 展开子级
                    expendChild(project, projectView, packagePath);
                });
            }
        }
    }

    private static void expendChild(Project project, ProjectView projectView, String packagePath) {
        AbstractProjectViewPane projectViewPane = projectView.getCurrentProjectViewPane();
        if (Objects.isNull(projectViewPane)) return;
        JTree tree = projectViewPane.getTree();

        TreeModel model = tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        Enumeration<TreeNode> nodes = root.depthFirstEnumeration();
        TreeNode selectedTreeNode = null;
        while (nodes.hasMoreElements()) {
            TreeNode treeNode = nodes.nextElement();
            if (treeNode.toString().equals(packagePath)) {
                selectedTreeNode = treeNode;
                break;
            }
        }

        if (Objects.nonNull(selectedTreeNode)) {
            TreePath selectionPath = new TreePath(selectedTreeNode);
            tree.expandPath(selectionPath);
            expandAllChildNodes(tree, selectionPath, packagePath);
        }
    }

    private static void expandAllChildNodes(JTree tree, TreePath parentPath, String packagePath) {
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parentPath.getLastPathComponent();
        for (int i = 0; i < parentNode.getChildCount(); i++) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) parentNode.getChildAt(i);
            if (childNode.getChildCount() > 0) {
                TreePath childPath = parentPath.pathByAddingChild(childNode);
                tree.expandPath(childPath);
                expandAllChildNodes(tree, childPath, packagePath);
            }
        }

        tree.expandPath(parentPath);
    }

    private static PsiDirectory findSubDirectory(PsiDirectory targetDirectory, String packagePath) {
        packagePath = packagePath.replaceAll("\\.", "/").replaceAll("\\\\", "/");
        String[] packages = packagePath.split("/");
        for (String packageName : packages) {
            PsiDirectory subdirectory = targetDirectory.findSubdirectory(packageName);
            if (Objects.nonNull(subdirectory)) targetDirectory = subdirectory;
        }

        return targetDirectory;
    }
}
