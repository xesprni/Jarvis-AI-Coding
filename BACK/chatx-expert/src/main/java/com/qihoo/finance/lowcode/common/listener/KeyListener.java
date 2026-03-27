package com.qihoo.finance.lowcode.common.listener;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * KeyListener
 *
 * @author fengjinfu-jk
 * date 2023/12/28
 * @version 1.0.0
 * @apiNote KeyListener
 */
public class KeyListener {
    public static KeyAdapter inputCheck(){
        return new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                // 校验输入是否为英文字母、数字或下划线
                if (!Character.isLetterOrDigit(c) && c != '_') {
                    // 如果输入不符合要求，则取消输入
                    e.consume();
                }
            }
        };
    }
}
