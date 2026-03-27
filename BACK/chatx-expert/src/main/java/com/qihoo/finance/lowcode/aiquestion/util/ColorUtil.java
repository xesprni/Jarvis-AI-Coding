package com.qihoo.finance.lowcode.aiquestion.util;

import com.intellij.ui.JBColor;
import com.qihoo.finance.lowcode.common.util.EditorComponentUtils;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;

import java.awt.*;

public class ColorUtil {

    public static final Color LINE = new JBColor(new Color(100, 100, 100), new Color(200, 200, 200));
    public static final Color LIGHT_REPLY_CONTENT = new Color(245, 245, 245);
    public static final Color DARK_REPLY_CONTENT = JBColor.background().brighter();
    public static final Color LIGHT_CODE_TITLE = new Color(180, 180, 180);
    public static final Color LIGHT_CODE_CONTENT = new Color(240, 240, 240);

    public static final Color PLUGIN_REPLY = new JBColor(new Color(230, 220, 220), JBColor.background().darker());
    public static final Color USER_REPLY = new JBColor(new Color(210, 230, 230), JBColor.background().brighter());

    public static Color getReplyContentBackground(String username) {
        if (ChatxApplicationSettings.settings().pluginName.equals(username)) {
            return PLUGIN_REPLY;
        }
        return USER_REPLY;
    }

    public static Color getCodeTitleBackground() {
        Color background = JBColor.background();
        boolean isBright = isBright(background);
        if (isBright) {
            return LIGHT_CODE_TITLE;
        } else {
//            return background.darker().darker();
            return background;
        }
    }

    public static Color getCodeContentBackground() {
        Color background = JBColor.background();
        boolean isBright = isBright(background);
        if (isBright) {
            return LIGHT_CODE_CONTENT;
        } else {
//            return background.darker().darker().darker();
            return EditorComponentUtils.BACKGROUND;
        }
    }

    public static boolean isBright(Color color) {
        double y = 0.229 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
        return y > 125;
    }

    public static Color getBorderLine() {
        if (JBColor.isBright()) {
            return JBColor.background().darker();
        } else {
            return JBColor.background().brighter();
        }
    }

}
