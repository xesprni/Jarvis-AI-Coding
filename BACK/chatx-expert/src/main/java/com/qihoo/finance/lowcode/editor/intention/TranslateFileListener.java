//package com.qihoo.finance.lowcode.editor.intention;
//
//import com.intellij.openapi.actionSystem.ActionManager;
//import com.intellij.openapi.application.ApplicationListener;
//import com.intellij.openapi.editor.Editor;
//import com.intellij.openapi.fileEditor.FileEditor;
//import com.intellij.openapi.fileEditor.FileEditorManager;
//import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
//import com.intellij.openapi.fileEditor.FileEditorManagerListener;
//import com.intellij.openapi.project.ProjectManagerListener;
//import com.intellij.openapi.vfs.VirtualFile;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Objects;
//
///**
// * TranslateListener
// *
// * @author fengjinfu-jk
// * date 2024/7/23
// * @version 1.0.0
// * @apiNote TranslateListener
// */
//public class TranslateFileListener implements FileEditorManagerListener, ProjectManagerListener, ApplicationListener {
//
//    @Override
//    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
//        FileEditorManagerListener.super.fileOpened(source, file);
//        Editor editor = source.getSelectedTextEditor();
//        if (Objects.isNull(editor)) return;
//
//        TranslateAction.registerShortcut(editor.getComponent());
//    }
//
//    @Override
//    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
//        FileEditorManagerListener.super.fileOpened(source, file);
//        Editor editor = source.getSelectedTextEditor();
//        if (Objects.isNull(editor)) return;
//
//        TranslateAction.unregisterShortcut(editor.getComponent());
//    }
//
//    @Override
//    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
//        FileEditorManagerListener.super.selectionChanged(event);
//        Editor editor = event.getManager().getSelectedTextEditor();
//        if (Objects.isNull(editor)) return;
//
//        TranslateAction.registerShortcut(editor.getComponent());
//    }
//}
