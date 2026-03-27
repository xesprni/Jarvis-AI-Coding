package com.qihoo.finance.lowcode.aiquestion.ui.view;

import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.ui.awt.RelativePoint;
import com.qihoo.finance.lowcode.aiquestion.ui.factory.InputPanelFactory;
import com.qihoo.finance.lowcode.aiquestion.util.ColorUtil;
import com.qihoo.finance.lowcode.common.entity.dto.askai.ShortcutInstructionInfo;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;

/**
 * ToolsPopup
 *
 * @author fengjinfu-jk
 * date 2024/4/10
 * @version 1.0.0
 * @apiNote ToolsPopup
 */
public class ActionInstructionPopup {
    private static long lastShowTime = 0;
    private final InputPanelFactory inputPanelFactory;
    private JBPopup popup;

    public ActionInstructionPopup(InputPanelFactory inputPanelFactory) {
        this.inputPanelFactory = inputPanelFactory;
    }

    public void show() {
        if (System.currentTimeMillis() - lastShowTime < 500) return;
        if (!LowCodeAppUtils.isLogin()) {
            return;
        }
        lastShowTime = System.currentTimeMillis();

        // show popup
        JPanel locationComponent = inputPanelFactory.getComponent();
        Dimension preferredSize = inputPanelFactory.getInput().getPreferredSize();
        JComponent popupComponent = createPanel(preferredSize);
        popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(popupComponent, null)
                .setBorderColor(null)
                .setShowBorder(false)
                .setShowShadow(false)
                .createPopup();

        popup.getContent().setOpaque(false);
        popup.show(new RelativePoint(locationComponent, new Point(8, 5 - (popupComponent.getPreferredSize().height))));
    }

    private void closePopup() {
        if (Objects.nonNull(popup) && !popup.isDisposed()) {
            popup.setUiVisible(false);
            popup.dispose();
        }
    }

    private JComponent createPanel(Dimension maxSize) {
        JPanel shortcutContent = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        int widthMedian = 405;
        int width = (maxSize.width > (widthMedian - 100) && maxSize.width < (widthMedian + 100)) ? maxSize.width - 10 : widthMedian;
        shortcutContent.setPreferredSize(new Dimension(width, 230));

        JPanel shortcutPanel = new JPanel(new GridLayout(2, 3));
        shortcutPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        shortcutContent.add(shortcutPanel);

        List<ShortcutInstructionInfo> actionInstructions = ChatxApplicationSettings.settings().shortcutInstructions.stream()
                .filter(s -> s.isInstruction(ShortcutInstructionInfo.InstructionType.ACTION_INSTRUCTION))
                .toList();

        for (ShortcutInstructionInfo instruction : actionInstructions) {
            shortcutPanel.add(toolButton(instruction));
        }

        for (int i = 0; i < (6 - actionInstructions.size()); i++) {
            shortcutPanel.add(defaultHolder());
        }

        // ask me
        JLabel holder = holderLabel("想拥有更多快捷功能, 请联系我们 ");
        holder.setIcon(Icons.scaleToWidth(Icons.ROCKET, 16));
        shortcutContent.add(holder);

        JPanel content = new JPanel();
        Border paddingBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);
        Border lineBorder = new RoundedLineBorder(ColorUtil.getBorderLine(), 20);
        content.setBorder(BorderFactory.createCompoundBorder(paddingBorder, lineBorder));
        content.add(shortcutContent);

        return content;
    }

    private void instructAction(ShortcutInstructionInfo instruction) {
        // desc
        inputPanelFactory.getFastCommand().setText(instruction.getShortDesc());
        inputPanelFactory.getNonCommandBtn().setVisible(true);
        // icon
//        Icons.asyncSetUrlIcon(inputPanelFactory.getFastCommand(), instruction.getIconUrl16(), Icons.AGENT_TASK2, 16);
        // 切换助手
        if (StringUtils.isNotEmpty(instruction.getAssistantCode())) {
            inputPanelFactory.flushAssistant(instruction.getAssistantCode());
        }
        // action
        if (StringUtils.isNotEmpty(instruction.getPrompt())) {
            String question = "/" + instruction.getName();
            if (instruction.isPackagePrompt()) {
                inputPanelFactory.setText(inputPanelFactory.packagePrompt(instruction, question));
            } else {
                inputPanelFactory.setText(question);
                // 存在prompt时, 追加 "/" 触发命令
                inputPanelFactory.getSendBtn().doClick();
            }
        }

        // close popup
        closePopup();
    }

    @NotNull
    private static JLabel holderLabel(String text) {
        JLabel holder = new JLabel();
        holder.setHorizontalAlignment(SwingConstants.CENTER);
        holder.setHorizontalTextPosition(JLabel.LEFT);
        holder.setText(text);
        holder.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        holder.setForeground(JBColor.GRAY);
        return holder;
    }

    private static final Dimension size = new Dimension(40, 40);

    @NotNull
    private static JButton defaultHolder() {
        JButton toolButton = new JButton();
        toolButton.setHorizontalAlignment(SwingConstants.CENTER);
        toolButton.setIcon(Icons.scaleToWidth(Icons.ADD, 30));
        toolButton.setBorderPainted(true);
        toolButton.setContentAreaFilled(false);
        toolButton.setPreferredSize(size);
        Border paddingBorder = BorderFactory.createEmptyBorder(10, 20, 10, 20);
        Border lineBorder = new RoundedLineBorder(ColorUtil.getBorderLine(), 20);
        toolButton.setBorder(BorderFactory.createCompoundBorder(paddingBorder, lineBorder));
        toolButton.setEnabled(false);

        return toolButton;
    }

    @NotNull
    private JComponent toolButton(ShortcutInstructionInfo instruction) {
        String name = instruction.getShortDesc();

        JButton toolButton = new JButton();
        toolButton.setHorizontalAlignment(SwingConstants.CENTER);
        // icon 30
//        Icons.asyncSetUrlIcon(toolButton, instruction.getIconUrl32(), Icons.AGENT_TASK2, 30);
        toolButton.setBorderPainted(false);
        toolButton.setContentAreaFilled(false);
        toolButton.setPreferredSize(size);
        ActionListener actionListener = e -> instructAction(instruction);
        toolButton.addMouseListener(mouseAdapter(toolButton, actionListener));

        JPanel btnPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        btnPanel.add(toolButton);
        // name
        JLabel holderLabel = holderLabel(name);
        holderLabel.setForeground(null);
        holderLabel.addMouseListener(mouseAdapter(toolButton, actionListener));
        btnPanel.add(holderLabel);

        Border paddingBorder = BorderFactory.createEmptyBorder(10, 20, 10, 20);
        Border lineBorder = new RoundedLineBorder(ColorUtil.getBorderLine(), 20);
        btnPanel.setBorder(BorderFactory.createCompoundBorder(paddingBorder, lineBorder));
        btnPanel.addMouseListener(mouseAdapter(toolButton, actionListener));
        if (StringUtils.isNotEmpty(instruction.getTips())) {
            toolButton.setToolTipText(instruction.getTips());
            holderLabel.setToolTipText(instruction.getTips());
            btnPanel.setToolTipText(instruction.getTips());
        }

        return btnPanel;
    }

    private static MouseAdapter mouseAdapter(JButton button, ActionListener actionListener) {
        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setContentAreaFilled(true);
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setContentAreaFilled(false);
                super.mouseExited(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                actionListener.actionPerformed(null);
                super.mouseClicked(e);
            }
        };
    }
}
