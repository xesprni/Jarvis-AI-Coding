/*
 * Copyright (c) 2018 David Boissier.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qihoo.finance.lowcode.console.mongo.view.action.result;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.console.mongo.view.ui.MongoPanel;
import com.qihoo.finance.lowcode.console.mongo.view.ui.MongoResultPanel;

import java.awt.event.KeyEvent;

public class DeleteMongoDocumentAction extends AnAction implements DumbAware {

    private final MongoResultPanel resultPanel;
    private final MongoPanel.ActionCallback actionCallback;

    public DeleteMongoDocumentAction(MongoResultPanel resultPanel, MongoPanel.ActionCallback actionCallback) {
        super("删除", "Delete this document", Icons.scaleToWidth(Icons.DELETE, 16));
        this.resultPanel = resultPanel;
        this.actionCallback = actionCallback;

        if (SystemInfo.isMac) {
            registerCustomShortcutSet(KeyEvent.VK_BACK_SPACE, 0, resultPanel);
        } else {
            registerCustomShortcutSet(KeyEvent.VK_DELETE, 0, resultPanel);
        }
    }

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        int check = Messages.showDialog("确认删除Document", "删除Document", new String[]{"是", "否"}, 1, Icons.scaleToWidth(Icons.WARN, 60));
        if (0 == check) {
            if (resultPanel.deleteSelectedMongoDocument()) {
                actionCallback.onOperationSuccess("删除Document.", "删除Document成功");
            }
        }
    }

    @Override
    public void update(AnActionEvent event) {
        event.getPresentation().setEnabled(resultPanel.isSelectedNodeId());
    }
}
