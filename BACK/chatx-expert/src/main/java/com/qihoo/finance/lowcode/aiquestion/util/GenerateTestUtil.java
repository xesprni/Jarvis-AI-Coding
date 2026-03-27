package com.qihoo.finance.lowcode.aiquestion.util;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.testIntegration.createTest.CreateTestAction;
import com.intellij.testIntegration.createTest.CreateTestUtils;
import com.qihoo.finance.lowcode.common.util.ApplicationUtil;
import com.qihoo.finance.lowcode.common.util.AssertUtil;
import com.qihoo.finance.lowcode.convertor.util.PsiUtil;
import lombok.SneakyThrows;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class GenerateTestUtil {

    @SneakyThrows
    public static void createTestFile(String content) {
        Project project = ApplicationUtil.findCurrentProject();
        AssertUtil.notNull(project, "Project is null");
        Editor editor = EditorUtil.getSelectedEditor(project);
        AssertUtil.notNull(editor, "Editor is null");
        PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        AssertUtil.notNull(psiFile, "PsiFile is null");
        PsiClass psiClass = PsiUtil.getPublicClass((PsiJavaFile) psiFile);
        AssertUtil.notNull(psiFile, "PsiClass is null");
        String className = psiClass.getName();
        Module srcModule = ModuleUtilCore.findModuleForFile(psiFile);
        AssertUtil.notNull(srcModule, "Src Module is null");
        PsiDirectory srcDir = psiFile.getContainingDirectory();
        PsiPackage srcPackage = JavaDirectoryService.getInstance().getPackage(srcDir);
        AssertUtil.notNull(srcPackage, "Src Package is null");
        Module testModule = CreateTestAction.suggestModuleForTests(project, srcModule);
        AssertUtil.notNull(testModule, "Test Module is null");
        List<VirtualFile> testRootUrls = CreateTestUtils.computeTestRoots(testModule);
        AssertUtil.assertTrue(!testRootUrls.isEmpty(), "Test root urls is empty");
        VirtualFile testRoot = testRootUrls.get(0);
        String packageName = srcPackage.getQualifiedName();
        String testClassFileName = className + "Test.java";
        // create new test file
        WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                VirtualFile parentDir = VfsUtil.createDirectoryIfMissing(testRoot, packageName.replaceAll("\\.", "/"));
                VirtualFile testFile = parentDir.findChild(testClassFileName);
                if (testFile == null) {
                    VirtualFile newFile = parentDir.createChildData(testRoot, className + "Test.java");
                    try (OutputStream outputStream = newFile.getOutputStream(testRoot)) {
                        outputStream.write(content.getBytes(StandardCharsets.UTF_8));
                    }
                    PsiUtil.navigateInEditor(project, newFile);
                } else {
                    // open diff window
                    EditorUtil.showDiff(project, testFile, content);
                }
            } catch (Exception e) {
                throw new RuntimeException("创建文件失败");
            }
        });
    }

    @SneakyThrows
    private static VirtualFile createDirectoryIfNeed(VirtualFile root, String packageName) {
        String[] packageNames = packageName.split("\\.");
        VirtualFile currentDir = root;
        for (String dirName : packageNames) {
            VirtualFile childDir = currentDir.findChild(dirName);
            if (childDir == null) {
                childDir = currentDir.createChildDirectory(currentDir, dirName);
            }
            currentDir = childDir;
        }
        return currentDir;
    }

    private static PsiDirectory createPackageDirectoryIfNotExists(PsiDirectory parentDirectory, String packageName) {
        PsiDirectory currentDirectory = parentDirectory;
        String[] packageNames = packageName.split("\\.");
        for (String name : packageNames) {
            PsiDirectory subDirectory = currentDirectory.findSubdirectory(name);
            if (subDirectory == null) {
                subDirectory = currentDirectory.createSubdirectory(name);
            }
            currentDirectory = subDirectory;
        }
        return currentDirectory;
    }
}
