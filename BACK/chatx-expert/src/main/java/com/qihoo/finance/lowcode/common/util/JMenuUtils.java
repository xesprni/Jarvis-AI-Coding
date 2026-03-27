package com.qihoo.finance.lowcode.common.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * JMenuUtils
 *
 * @author fengjinfu-jk
 * date 2023/11/3
 * @version 1.0.0
 * @apiNote JMenuUtils
 */
public class JMenuUtils {
    public static JBMenuItem createDeleteMenu(Project project, String text, @NotNull Runnable executeDelete) {
        String tips = "您正在执行删除操作 \n\n请输入 “确认删除” 完成删除 !";
        return createDeleteMenu(project, text, tips, executeDelete);
    }

    public static JBMenuItem createDeleteMenu(Project project, String text, String tips, @NotNull Runnable executeDelete) {
        String sure = "确认删除";
        JBMenuItem menuItem = new JBMenuItem(new AbstractAction(text) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 自定义菜单项的操作
                String inputString = Messages.showInputDialog(project, tips, text, Icons.scaleToWidth(Icons.DELETE, 60), null, new InputValidator() {
                    @Override
                    public boolean checkInput(@NlsSafe String inputString) {
                        return sure.equalsIgnoreCase(inputString);
                    }

                    @Override
                    public boolean canClose(@NlsSafe String inputString) {
                        return true;
                    }
                });

                if (sure.equalsIgnoreCase(inputString)) {
                    executeDelete.run();
                }
            }
        });

        // 设置自定义边距
        menuItem.setMargin(JBUI.insets(0, 10));
        return menuItem;
    }
}
