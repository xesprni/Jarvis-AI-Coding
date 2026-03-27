package com.qihoo.finance.lowcode.declarative.ui;

import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.impl.DiffRequestProcessor;
import com.intellij.diff.impl.DiffWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.WindowWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MyDiffWindow
 *
 * @author fengjinfu-jk
 * date 2024/4/23
 * @version 1.0.0
 * @apiNote MyDiffWindow
 */
public class DeclareDiffWindow extends DiffWindow {
    public DeclareDiffWindow(@Nullable Project project, @NotNull DiffRequestChain requestChain, @NotNull DiffDialogHints hints) {
        super(project, requestChain, hints);
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    public WindowWrapper getWrapper() {
        return super.getWrapper();
    }

    @Override
    public DiffRequestProcessor getProcessor() {
        return super.getProcessor();
    }
}
