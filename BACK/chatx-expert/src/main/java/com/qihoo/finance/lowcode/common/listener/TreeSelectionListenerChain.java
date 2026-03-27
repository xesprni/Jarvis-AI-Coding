package com.qihoo.finance.lowcode.common.listener;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.qihoo.finance.lowcode.common.configuration.UserContextPersistent;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.util.DataContext;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.Objects;

/**
 * @author fengjinfu-jk
 * date 2023/8/7
 * @version 1.0.0
 * @apiNote ListenerChain
 */
public abstract class TreeSelectionListenerChain implements TreeSelectionListener {
    private TreeSelectionListener listener;

    protected Project project;

    public TreeSelectionListenerChain(@NotNull Project project) {
        this.project = project;
    }

    public TreeSelectionListener nextListener() {
        return null;
    }

    public abstract void handlerValueChanged(TreeSelectionEvent e);

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        handlerValueChanged(e);
        loadNextListener(e);
    }

    private void loadNextListener(TreeSelectionEvent e) {
        if (Objects.isNull(listener)) {
            listener = nextListener();
        }

        if (Objects.nonNull(listener)) {
            listener.valueChanged(e);
        }
    }

    protected static DefaultMutableTreeNode currentNode(TreeSelectionEvent e) {
        TreePath selectionPath = e.getNewLeadSelectionPath();
        selectionPath = ObjectUtils.defaultIfNull(selectionPath, e.getOldLeadSelectionPath());
        Object component = selectionPath.getLastPathComponent();
        return (DefaultMutableTreeNode) component;
    }
}
