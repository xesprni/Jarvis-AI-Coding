package com.qihoo.finance.lowcode.gentracker.tool;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestFactory;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

/**
 * 文件对比工具
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public class CompareFileUtils {

    /**
     * 显示文件对比框
     *
     * @param project   项目
     * @param leftFile  左边的文件
     * @param rightFile 右边的文件
     */
    public static void showCompareWindow(Project project, VirtualFile leftFile, VirtualFile rightFile) {

        try {
            Class<?> cls = Class.forName("com.intellij.diff.actions.impl.MutableDiffRequestChain");
            // 新版支持
            DiffContentFactory contentFactory = DiffContentFactory.getInstance();
            DiffRequestFactory requestFactory = DiffRequestFactory.getInstance();

            DiffContent leftContent = contentFactory.create(project, leftFile);
            DiffContent rightContent = contentFactory.create(project, rightFile);

            DiffRequestChain chain = (DiffRequestChain) cls.getConstructor(DiffContent.class, DiffContent.class).newInstance(leftContent, rightContent);

            cls.getMethod("setWindowTitle", String.class).invoke(chain, requestFactory.getTitle(leftFile, rightFile));
            cls.getMethod("setTitle1", String.class).invoke(chain, requestFactory.getContentTitle(leftFile));
            cls.getMethod("setTitle2", String.class).invoke(chain, requestFactory.getContentTitle(rightFile));
            DiffManager.getInstance().showDiff(project, chain, DiffDialogHints.MODAL);
        } catch (ClassNotFoundException e) {
            // 旧版兼容
            DiffRequest diffRequest = DiffRequestFactory.getInstance().createFromFiles(project, leftFile, rightFile);
            DiffManager.getInstance().showDiff(project, diffRequest, DiffDialogHints.MODAL);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                 InvocationTargetException e) {
            ExceptionUtil.rethrow(e);
        }
    }

    /**
     * 显示文件对比框,关闭后统一获取修改内容
     *
     * @param project   项目
     * @param leftFile  左边的文件
     * @param rightFile 右边的文件(可编辑)
     * @param callback  对比窗口关闭后的回调,传入修改后的内容
     */
    public static void showCompareWindowWithCallback(Project project, VirtualFile leftFile, VirtualFile rightFile, Consumer<String> callback) {

        try {
            Class<?> cls = Class.forName("com.intellij.diff.actions.impl.MutableDiffRequestChain");
            DiffContentFactory contentFactory = DiffContentFactory.getInstance();
            DiffRequestFactory requestFactory = DiffRequestFactory.getInstance();

            DiffContent leftContent = contentFactory.create(project, leftFile);
            if (leftContent instanceof DocumentContent) {
                // 设置左边的文件为只读
                leftContent.putUserData(DiffUserDataKeys.FORCE_READ_ONLY, true);
            }
            DiffContent rightContent = contentFactory.create(project, rightFile);
            if (rightContent instanceof DocumentContent) {
                Document rightDocument = ((DocumentContent) rightContent).getDocument();
                rightDocument.addDocumentListener(new DocumentListener() {
                    @Override
                    public void documentChanged(@NotNull DocumentEvent event) {
                        callback.accept(rightDocument.getText());
                    }
                });
            }
            DiffRequestChain chain = (DiffRequestChain) cls.getConstructor(DiffContent.class, DiffContent.class).newInstance(leftContent, rightContent);

            cls.getMethod("setWindowTitle", String.class).invoke(chain, requestFactory.getTitle(leftFile, rightFile));
            cls.getMethod("setTitle1", String.class).invoke(chain, requestFactory.getContentTitle(leftFile));
            cls.getMethod("setTitle2", String.class).invoke(chain, requestFactory.getContentTitle(rightFile));
            DiffManager.getInstance().showDiff(project, chain, DiffDialogHints.MODAL);
        } catch (ClassNotFoundException e) {
            DiffRequest diffRequest = DiffRequestFactory.getInstance().createFromFiles(project, leftFile, rightFile);
            DiffManager.getInstance().showDiff(project, diffRequest, DiffDialogHints.MODAL);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                 InvocationTargetException e) {
            ExceptionUtil.rethrow(e);
        } catch (Exception e) {
            ExceptionUtil.rethrow(e);
        }
    }

}
