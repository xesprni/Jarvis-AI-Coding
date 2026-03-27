package com.qihoo.finance.lowcode.aiquestion.ui.factory;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.plugins.PluginManagerMain;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.qihoo.finance.lowcode.aiquestion.ui.QuestionPanel;
import com.qihoo.finance.lowcode.aiquestion.ui.component.HorizontalScrollBarEditor;
import com.qihoo.finance.lowcode.aiquestion.ui.component.NoWrapJTextPane;
import com.qihoo.finance.lowcode.aiquestion.util.CodeUtil;
import com.qihoo.finance.lowcode.aiquestion.util.ColorUtil;
import com.qihoo.finance.lowcode.aiquestion.util.MarkdownParser;
import com.qihoo.finance.lowcode.common.util.ApplicationUtil;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;

@Slf4j
public class TextPaneFactory {

    public JTextPane createTextPane(String msg, Color backgroundColor) {
        msg = MarkdownParser.parseMarkdown(msg);

        int margin = 5;
        JTextPane textPane = createTextPane();

        textPane.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
        textPane.setEditable(false);
        textPane.setBackground(backgroundColor);
        textPane.setContentType("text/html");
        textPane.setFont(UIManager.getFont("Label.font"));
        HTMLEditorKit kit = new HTMLEditorKit();
        textPane.setEditorKit(kit);
        textPane.setText(msg);
        textPane.addHyperlinkListener(new PluginManagerMain.MyHyperlinkListener());
        return textPane;
    }

    @NotNull
    private static JTextPane createTextPane() {
        QuestionPanel questionPanel = ProjectUtils.getCurrProject().getService(QuestionPanel.class);
        return new JTextPane() {
            @Override
            public void scrollRectToVisible(Rectangle aRect) {
                questionPanel.scrollBottom(true);
            }
        };
    }

    public JTextPane createCodePane(String msg, Color backgroundColor) {
        int margin = 5;
        JTextPane textPane = new NoWrapJTextPane();
        textPane.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
        textPane.setEditable(false);
        textPane.setBackground(backgroundColor);
        CodeUtil.renderCode(textPane, msg);
        return textPane;
    }

    public EditorTextField createEditor(String msg) {
        Project project = ApplicationUtil.findCurrentProject();
        EditorTextField textField = new HorizontalScrollBarEditor(msg, project, JavaFileType.INSTANCE);
        textField.setOneLineMode(false);
        textField.setViewer(true);
        textField.setBackground(ColorUtil.getCodeContentBackground());
        return textField;
    }
}
