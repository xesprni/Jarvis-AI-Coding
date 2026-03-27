package com.qihoo.finance.lowcode.aiquestion.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.JBColor;
import com.intellij.ui.RoundedLineBorder;
import com.qihoo.finance.lowcode.aiquestion.ui.factory.InputPanelFactory;
import com.qihoo.finance.lowcode.aiquestion.util.ColorUtil;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.entity.dto.askai.ShortcutInstructionInfo;
import com.qihoo.finance.lowcode.common.factory.ChatXToolWindowFactory;
import com.qihoo.finance.lowcode.common.ui.TabPanel;
import com.qihoo.finance.lowcode.common.ui.ToolBarPanel;
import com.qihoo.finance.lowcode.common.ui.base.RoundedLabel;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JButtonUtils;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * AskAiMainPanel
 *
 * @author fengjinfu-jk
 * date 2024/4/8
 * @version 1.0.0
 * @apiNote AskAiMainPanel
 */
public class AskAiMainPanel extends JPanel {
    private final JPanel loadingPanel;
    private final JComponent title;
    private final JComponent shortcut;
    private final JComponent tips;

    public AskAiMainPanel(JPanel loading) {
        // layout
        setLayout(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        // loading
        this.loadingPanel = loading;
        add(loading);
        // title
        this.title = createTitle();
        add(title);
        // content
//        add(createContent());
        // shortcut
        this.shortcut = createShortcut();
        add(shortcut);
        // tips
        this.tips = createTips();
        add(tips);
        setLoading(false);
    }

    public void setLoading(boolean loading) {
        int top = Math.max(10, ChatXToolWindowFactory.getToolWindow().getComponent().getSize().height / 3);
        loadingPanel.setBorder(BorderFactory.createEmptyBorder(top, 0, 0, 0));

        this.loadingPanel.setVisible(loading);
        this.title.setVisible(!loading);
        this.shortcut.setVisible(!loading);
        this.tips.setVisible(!loading);
    }

    public static JComponent createShortcut() {
        JPanel shortcutContent = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        JPanel shortcutPanel = new JPanel(new GridLayout(-1, 2));
        shortcutContent.add(shortcutPanel);

        {
            // function tips
            JLabel shortcutTips1 = new JLabel();
            shortcutTips1.setHorizontalAlignment(SwingConstants.CENTER);
            if (SystemInfo.isWindows) {
                shortcutTips1.setText("Ctrl");
                shortcutTips1.setFont(new Font("微软雅黑", Font.BOLD, 12));
            } else {
                shortcutTips1.setText("Control");
                shortcutTips1.setFont(new Font("微软雅黑", Font.BOLD, 12));
            }

            // function tips
            JLabel shortcutTips2 = new JLabel();
            shortcutTips2.setIcon(Icons.scaleToWidth(Icons.VK_1, 15));

            JLabel shortcutTips3 = new JLabel();
            shortcutTips3.setText("全局唤醒Jarvis");
            shortcutTips3.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            shortcutTips3.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));

            JPanel shortcutPanel1 = flowComponents(true, shortcutTips1, shortcutTips2);
            shortcutPanel.add(shortcutPanel1);
            shortcutPanel.add(shortcutTips3);
        }

        {
            // function tips
            JLabel shortcutTips1 = new JLabel();
            shortcutTips1.setHorizontalAlignment(SwingConstants.LEFT);
            shortcutTips1.setText("Tab");
            shortcutTips1.setFont(new Font("微软雅黑", Font.BOLD, 12));

            JLabel shortcutTips11 = new JLabel();
            shortcutTips11.setText("应用补全");
            shortcutTips11.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            JPanel shortcutPanel1 = flowComponents(shortcutTips1, shortcutTips11);

            // function tips
            JLabel shortcutTips2 = new JLabel();
            shortcutTips2.setHorizontalAlignment(SwingConstants.LEFT);
            shortcutTips2.setText("Esc");
            shortcutTips2.setFont(new Font("微软雅黑", Font.BOLD, 12));

            JLabel shortcutTips22 = new JLabel();
            shortcutTips22.setText("取消补全");
            shortcutTips22.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            JPanel shortcutPanel2 = flowComponents(shortcutTips2, shortcutTips22);

            shortcutPanel2.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 0));
            shortcutPanel.add(shortcutPanel1);
            shortcutPanel.add(shortcutPanel2);
        }
        {
            // function tips
            JLabel shortcutTips1 = ctrl();
            // function tips
            JLabel shortcutTips2 = new JLabel();
            shortcutTips2.setIcon(Icons.scaleToWidth(Icons.RIGHT, 13));

            JLabel shortcutTips3 = new JLabel();
            shortcutTips3.setText("按词应用补全");
            shortcutTips3.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            shortcutTips3.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));
            // function tips
            JLabel shortcutTips11 = ctrl();
            // function tips
            JLabel shortcutTips12 = alt();

            JLabel shortcutTips13 = new JLabel();
            shortcutTips13.setIcon(Icons.scaleToWidth(Icons.RIGHT, 13));

            JLabel shortcutTips14 = new JLabel();
            shortcutTips14.setText("按行应用补全");
            shortcutTips14.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            shortcutTips14.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));

            JPanel shortcutPanel1 = flowComponents(true, shortcutTips1, shortcutTips2);
            JPanel shortcutPanel2 = flowComponents(true, shortcutTips11, shortcutTips12, shortcutTips13);

            shortcutPanel.add(shortcutPanel1);
            shortcutPanel.add(shortcutTips3);
            shortcutPanel.add(shortcutPanel2);
            shortcutPanel.add(shortcutTips14);
        }

        // ask mel
        JLabel holder = new JLabel();
        holder.setHorizontalAlignment(SwingConstants.CENTER);
        holder.setText("记住快捷键, 畅享AI辅助编码");
        holder.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        holder.setHorizontalTextPosition(JLabel.LEFT);
        holder.setIcon(Icons.scaleToWidth(Icons.ROCKET, 18));
        holder.setForeground(JBColor.GRAY);
        holder.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        shortcutContent.add(holder);

        JPanel content = new JPanel();
        content.add(shortcutContent);

        Border paddingBorder = BorderFactory.createEmptyBorder(20, 15, 20, 15);
        Border lineBorder = new RoundedLineBorder(ColorUtil.getBorderLine(), 20);
        content.setBorder(BorderFactory.createCompoundBorder(paddingBorder, lineBorder));
        return content;
    }

    @NotNull
    private static JLabel alt() {
        JLabel shortcutTips12 = new JLabel();
        shortcutTips12.setText(SystemInfo.isWindows ? "Alt " : "Control");
        shortcutTips12.setFont(new Font("微软雅黑", Font.BOLD, 12));
        return shortcutTips12;
    }

    @NotNull
    private static JLabel ctrl() {
        JLabel shortcutTips11 = new JLabel();
        shortcutTips11.setHorizontalAlignment(SwingConstants.CENTER);
        if (SystemInfo.isWindows) {
            shortcutTips11.setText("Ctrl");
            shortcutTips11.setFont(new Font("微软雅黑", Font.BOLD, 12));
        } else {
            shortcutTips11.setIcon(Icons.scaleToWidth(Icons.COMMAND, 13));
        }
        return shortcutTips11;
    }

    private static JPanel flowComponents(boolean split, JComponent... components) {
        JPanel flowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JComponent component : components) {
            flowPanel.add(component);
            if (split) {
                flowPanel.add(new JLabel(Icons.scaleToWidth(Icons.ADD_ICON, 13)));
            }
        }
        if (split && flowPanel.getComponentCount() >= 2) {
            flowPanel.remove(flowPanel.getComponentCount() - 1);
        }

        return flowPanel;
    }

    private static JPanel flowComponents(JComponent... components) {
        return flowComponents(false, components);
    }

    private JComponent createTips() {
        JPanel tips = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));
        // ask me
        JLabel holder = new JLabel();
        holder.setHorizontalAlignment(SwingConstants.CENTER);
        holder.setText("可以试着问我");
        holder.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        holder.setHorizontalTextPosition(JLabel.LEFT);
        holder.setIcon(Icons.scaleToWidth(Icons.QUESTION, 22));
        holder.setForeground(JBColor.GRAY);
        tips.add(holder);
        // 快捷指令
        addFastCommand(tips);
        // ask me
        tips.add(getHelpDoc());
        tips.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));
        return tips;
    }

    @NotNull
    public static JLabel getHelpDoc() {
        JLabel more = new JLabel();
        more.setHorizontalAlignment(SwingConstants.CENTER);
        String moreTxt = String.format("<html>%s&nbsp;&nbsp;<u style=\"color: rgb(88,157,246);\">Jarvis帮助文档</u></html>", "了解更多智能编码能力, 请查看");
        more.setText(moreTxt);
        more.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        more.setForeground(JBColor.GRAY);
        more.setCursor(new Cursor(Cursor.HAND_CURSOR));
        more.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
//        more.setHorizontalTextPosition(JLabel.LEFT);
        more.setIcon(Icons.scaleToWidth(Icons.HELP, 15));
        more.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ToolBarPanel toolBarPanel = ToolBarPanel.getInstance();
                BrowserUtil.browse(StringUtils.defaultIfBlank(toolBarPanel.getHelpDocHost(), Constants.Url.TOOL_HELP));
            }
        });
        return more;
    }

    private static final HashMap<String, String> innerCommand = new HashMap<>() {{
        put("Java的冒泡排序怎么实现", "Java冒泡排序");
        put("MySQL分页查询SQL语句怎么写", "SQL分页查询");
    }};
    private static final String[] innerCommandDesc = innerCommand.keySet().toArray(String[]::new);

    private void addFastCommand(JPanel tips) {
        final int maxCommand = 2;
        int commandCount = 0;
        final String ignoreCommand = "clear conversation";
        List<ShortcutInstructionInfo> fastCommand = ChatxApplicationSettings.settings().shortcutInstructions
                .stream().filter(s -> StringUtils.isNotEmpty(s.getPrompt())).toList();
        for (ShortcutInstructionInfo command : fastCommand) {
            if (command.getName().contains(ignoreCommand)) continue;
            if (commandCount >= maxCommand) break;

            // ask me
            commandCount++;
            tips.add(triggerCommand(command.getDesc(), "/" + command.getName()));
        }

        if (commandCount < maxCommand) {
            for (int i = 0; i < (maxCommand - commandCount); i++) {
                if (i > innerCommandDesc.length - 1) break;

                String desc = innerCommandDesc[i];
                tips.add(triggerCommand(desc, innerCommand.get(desc)));
            }
        }
    }

    @NotNull
    private static JLabel triggerCommand(String desc, String command) {
        JLabel commandAct = new JLabel();
        commandAct.setHorizontalAlignment(SwingConstants.CENTER);
        commandAct.setText(desc);
        commandAct.setIcon(Icons.scaleToWidth(Icons.STARTS, 15));
        commandAct.setForeground(JBColor.isBright() ? RoundedLabel.BLUE : JBColor.CYAN);
        commandAct.addMouseListener(fastAskQuestion(desc, command, commandAct));
        return commandAct;
    }

    @NotNull
    public static MouseAdapter fastAskQuestion(String desc, String command, JLabel question) {

        String uTxt = String.format("<html><u style=\"color: rgb(88,157,246);\">%s</u></html>", desc);
        return new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                question.setText(desc);
                super.mouseExited(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                question.setText(uTxt);
                super.mouseEntered(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                QuestionPanel questionPanel = ProjectUtils.getCurrProject().getService(QuestionPanel.class);
                if (Objects.nonNull(questionPanel)) {
                    InputPanelFactory inputPanelFactory = questionPanel.getInputPanelFactory();
                    inputPanelFactory.setText(command);
                    inputPanelFactory.getSendBtn().doClick();
                }
            }
        };
    }

    private JComponent createContent() {
        JPanel content = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP));

        JPanel mainContent = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Border iconBorder = BorderFactory.createEmptyBorder(0, 5, 0, 5);
        JButton db = JButtonUtils.createNonOpaqueButton(Icons.scaleToWidth(Icons.DB_GEN, 20), new Dimension(30, 30));
        JButton api = JButtonUtils.createNonOpaqueButton(Icons.scaleToWidth(Icons.API, 20), new Dimension(30, 30));
        JButton crv = JButtonUtils.createNonOpaqueButton(Icons.scaleToWidth(Icons.GIT_LAB, 24), new Dimension(30, 30));
        JButton ask = JButtonUtils.createNonOpaqueButton(Icons.scaleToWidth(Icons.ASK_AI, 20), new Dimension(30, 30));
        ask.setToolTipText("Ask AI");
        db.setToolTipText("数据库生成代码");
        api.setToolTipText("接口生成代码");
        crv.setToolTipText("代码评审");
        db.setBorder(iconBorder);
        api.setBorder(iconBorder);
        crv.setBorder(iconBorder);
        ask.setBorder(iconBorder);
        db.addActionListener(e -> TabPanel.setSelectIndex(TabPanel.DB_INDEX));
        api.addActionListener(e -> TabPanel.setSelectIndex(TabPanel.API_INDEX));
        crv.addActionListener(e -> TabPanel.setSelectIndex(TabPanel.CRV_INDEX));
        ask.addActionListener(e -> TabPanel.setSelectIndex(TabPanel.ASK_AI_INDEX));

        mainContent.add(db);
        mainContent.add(api);
        mainContent.add(crv);
        mainContent.add(ask);
        mainContent.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        JPanel mainContentWrap = new JPanel();
        mainContentWrap.add(mainContent);
        content.add(mainContentWrap);

        // function tips
        JLabel functionTips = new JLabel();
        functionTips.setHorizontalAlignment(SwingConstants.CENTER);
        functionTips.setText("更丰富的AI能力, 更贴近业务的编码辅助");
        functionTips.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        functionTips.setForeground(JBColor.GRAY);
        functionTips.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        content.add(functionTips);

        Border paddingBorder = BorderFactory.createEmptyBorder(25, 5, 25, 5);
        Border lineBorder = new RoundedLineBorder(JBColor.background().brighter(), 8);
        content.setBorder(BorderFactory.createCompoundBorder(paddingBorder, lineBorder));
        return content;
    }

    private JComponent createTitle() {
//        JLabel logo = new JLabel(Icons.scaleToWidth(Icons.LOGO, 40));
//        logo.setHorizontalAlignment(SwingConstants.CENTER);
//        logo.setBorder(BorderFactory.createEmptyBorder(30, 0, 0, 0));

        // welcome
        JLabel welcome = new JLabel();
        welcome.setHorizontalAlignment(SwingConstants.CENTER);
        // welcome.setText("欢迎使用JARVIS • AI软件研发");
        welcome.setHorizontalTextPosition(JLabel.LEFT);
        welcome.setIcon(Icons.scaleToWidth(Icons.JARVIS, 120));
        welcome.setFont(new Font("微软雅黑", Font.BOLD, 15));

        // holder
        JLabel holder = new JLabel();
        holder.setHorizontalAlignment(SwingConstants.CENTER);
        holder.setText(Constants.PLUGIN_SLOGAN);
        holder.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        holder.setForeground(JBColor.GRAY);
        holder.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JPanel titlePanel = new JPanel(new BorderLayout());
//        titlePanel.add(logo, BorderLayout.NORTH);
        titlePanel.add(welcome, BorderLayout.CENTER);
        titlePanel.add(holder, BorderLayout.SOUTH);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));
        return titlePanel;
    }
}
