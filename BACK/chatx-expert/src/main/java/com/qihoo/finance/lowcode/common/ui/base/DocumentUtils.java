package com.qihoo.finance.lowcode.common.ui.base;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * DocumentUtils
 *
 * @author fengjinfu-jk
 * date 2023/10/12
 * @version 1.0.0
 * @apiNote DocumentUtils
 */
public class DocumentUtils {
    public static DocumentFilter createDocumentFilter(String regex) {
        return new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                // 过滤非英文字符
                if (string.matches(regex)) {
                    super.insertString(fb, offset, string, attr);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                // 过滤非英文字符
                if (text.matches(regex)) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        };
    }
}
