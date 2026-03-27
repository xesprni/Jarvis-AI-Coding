package org.qifu.devops.ide.plugins.jiracommit.ui;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.ui.Refreshable;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * @author linshiyuan-jk
 */
@Slf4j
public class CommitHelperFormSwing implements KeyListener {

    private JPanel north = new JPanel();

    private JPanel center = new JPanel();

    private JPanel south = new JPanel();


    private JLabel name = new JLabel("属性1");
    private JTextField nameContent = new JTextField();

    private JLabel age = new JLabel("属性2");
    private JTextField ageContent = new JTextField();

    private JLabel activeList = new JLabel("活跃列表");
    private JList activeListContent = new JList();

    private JLabel activeList1 = new JLabel("活跃列表2");
    private JList activeListContent1 = new JList();
    JComboBox activeCombo = new JComboBox();

    private JLabel pipelineList = new JLabel("自动触发流水线");
    private JList pipelineListContent = new JList();


    private JComboBox cbx;
    private JTextField jtf;



    public JPanel initNorth() {

        //定义表单的标题部分，放置到IDEA会话框的顶部位置

        JLabel title = new JLabel("表单标题");
        title.setFont(new Font("微软雅黑", Font.PLAIN, 26)); //字体样式
        title.setHorizontalAlignment(SwingConstants.CENTER); //水平居中
        title.setVerticalAlignment(SwingConstants.CENTER); //垂直居中
        north.setSize(200,15);
        north.add(title);

        return north;
    }

    public JPanel initCenter() {

        //定义表单的主体部分，放置到IDEA会话框的中央位置

        //一个简单的2行2列的表格布局
        center.setLayout(new GridLayout(3, 2));

        //row2：姓名+文本框
        center.add(name);
        center.add(nameContent);

        //row3：年龄+文本框
        center.add(age);
        center.add(ageContent);


        cbx = new JComboBox(getItems());
        cbx.setEditable(true);
        cbx.setBounds(20, 20, 80, 30);
        jtf = (JTextField)cbx.getEditor().getEditorComponent();
        jtf.addKeyListener(this);
        center.add(cbx);

        //活跃列表
//        String[] contents = {"NREQUEST-10086 这是个标题 20230629","NREQUEST-10086 这是个标题 20230629","NREQUEST-10086 这是个标题 20230629","NREQUEST-10086 这是个标题 20230629"};
//        activeListContent = new JList<>(contents);
//        JScrollPane jScrollPane = new JScrollPane(activeListContent);
//        jScrollPane.setSize(400,300);
//        center.add(activeList);
//        center.add(jScrollPane);

//        //活跃列表2
//        activeCombo.addItem(null);
//        activeCombo.addItem("REQUEST-10086 这是个标题 20230629");
//        activeCombo.addItem("REQUEST-10086 这是个标题 20230709");
//        activeCombo.addItem("REQUEST-10086 这是个标题 20230209");
//
//        activeCombo.setSize(400,500);
//        center.add(activeList1);
//        center.add(activeCombo);

        center.setSize(400,800);

        return center;
    }

    public JPanel initSouth(AnActionEvent anActionEvent) {

        //定义表单的提交按钮，放置到IDEA会话框的底部位置

        JButton submit = new JButton("提交");
        submit.setHorizontalAlignment(SwingConstants.CENTER); //水平居中
        submit.setVerticalAlignment(SwingConstants.CENTER); //垂直居中
        submit.addActionListener(e -> {
                    String name = nameContent.getText();
                    String age = ageContent.getText();
//              int active1 = activeListContent.getDropLocation().getIndex();
//              String active2 = activeCombo.getSelectedItem().toString();
//              getCommitPanel(anActionEvent).setCommitMessage(name + age +active1+active2+"\r\n");
                    getCommitPanel(anActionEvent).setCommitMessage(name + age +cbx.getSelectedItem().toString()+"\r\n");
                    log.info("name: {}, age: {}", name, age);
                    north.setVisible(false);
                    center.setVisible(false);
                    south.setVisible(false);
                    north.getRootPane().setVisible(false);

                }
        );
        south.add(submit);
        JButton cancel = new JButton("取消");
        cancel.setHorizontalAlignment(SwingConstants.CENTER); //水平居中
        cancel.setVerticalAlignment(SwingConstants.CENTER); //垂直居中
        cancel.addActionListener(e -> {
                    north.setVisible(false);
                    center.setVisible(false);
                    south.setVisible(false);
                }
        );
        south.add(cancel);
        return south;
    }

    private void setDefaultCloseOperation() {

    }


    private CommitMessageI  getCommitPanel(AnActionEvent actionEvent) {
        if (Refreshable.PANEL_KEY.getData(actionEvent.getDataContext()).getClass().equals(CommitMessageI.class)){
            return (CommitMessageI) Refreshable.PANEL_KEY.getData(actionEvent.getDataContext());
        }
        return VcsDataKeys.COMMIT_MESSAGE_CONTROL.getData(actionEvent.getDataContext());
    }


    public Object[] getItems() {
        return new Object[] {
                "abcd", "acdef", "cdefg", "defg"
        };
    }

    @Override
    public void keyTyped(KeyEvent e) {
        cbx.showPopup();
        cbx.setPopupVisible(true);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        cbx.showPopup();
        cbx.setPopupVisible(true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        Object obj = e.getSource();
        if (obj == jtf) {
            String key = jtf.getText();
            cbx.removeAllItems();
            for (Object item : getItems()) {
                if (((String)item).contains(key)) { //这里是包含key的项目都筛选出来，可以把startsWith改成contains就是筛选以key开头的项目
                    cbx.addItem(item);
                }
            }
            jtf.setText(key);
        }
    }
}
