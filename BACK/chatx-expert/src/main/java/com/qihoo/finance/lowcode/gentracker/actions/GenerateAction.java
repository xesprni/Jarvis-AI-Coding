package com.qihoo.finance.lowcode.gentracker.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.design.constant.FieldTypeMatch;
import com.qihoo.finance.lowcode.design.entity.DatabaseColumnNode;
import com.qihoo.finance.lowcode.design.entity.MongoCollectionNode;
import com.qihoo.finance.lowcode.design.entity.MySQLTableNode;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.entity.MatchType;
import com.qihoo.finance.lowcode.gentracker.entity.TypeMapper;
import com.qihoo.finance.lowcode.gentracker.tool.CurrGroupUtils;
import com.qihoo.finance.lowcode.gentracker.tool.StringUtils;
import com.qihoo.finance.lowcode.gentracker.ui.dialog.MongoGenerateDialog;
import com.qihoo.finance.lowcode.gentracker.ui.dialog.MySQLGenerateDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 代码生成菜单
 *
 * @author fengjinfu-jk
 * @version 1.0.0
 * @since 2023/07/28
 */
public class GenerateAction extends AbstractAction {
    private final Project project;
    private int step;

    /**
     * 构造方法
     *
     * @param text 菜单名称
     */
    public GenerateAction(@Nullable String text, Project project, int step) {
        super(text);
        this.project = project;
        this.step = step;
    }

    public GenerateAction(@Nullable String text, Project project) {
        super(text);
        this.project = project;
        this.step = 0;
    }

    /**
     * 处理方法
     *
     * @param e 事件对象
     */
    @Override
    public void actionPerformed(@NotNull ActionEvent e) {
        if (project == null) {
            return;
        }

        JTree tree = DataContext.getInstance(project).getDbTree();
        TreePath[] selectionPaths = tree.getSelectionPaths();
        if (Objects.isNull(selectionPaths)) return;

        this.selectAndShowModal(tree, Arrays.stream(selectionPaths).iterator());
    }

    private void selectAndShowModal(JTree tree, Iterator<TreePath> iterator) {
        if (!iterator.hasNext()) return;

        step++;
        // 启用强制加载表下级信息, 防止字段信息不完整
        DataContext.getInstance(project).setMustSyncLoadDbTree(true);
        TreePath selectionPath = iterator.next();
        Object node = selectionPath.getLastPathComponent();
        // 选中以触发Listener
        tree.setSelectionPath(selectionPath);
        // 打开界面
        if (node instanceof MySQLTableNode) {
            // 加载字段信息, 确保代码生成时字段信息完整
            new MySQLGenerateDialog(project, false, step, () -> selectAndShowModal(tree, iterator)).show();
        } else if (node instanceof MongoCollectionNode) {
            new MongoGenerateDialog(project, false, step, () -> selectAndShowModal(tree, iterator)).show();
        }
        // 其他类型数据源...
        // 关闭强制同步查询
        DataContext.getInstance(project).setMustSyncLoadDbTree(false);
    }

    /**
     * 类型校验，如果存在未知类型则引导用于去条件类型
     *
     * @param dbTable 原始表对象
     * @return 是否验证通过
     */
    private boolean typeValidator(Project project, MySQLTableNode dbTable) {
        // 处理所有列
        List<DatabaseColumnNode> columns = dbTable.getTableColumns();
        List<TypeMapper> typeMapperList = CurrGroupUtils.getCurrTypeMapperGroup().getElementList();

        // 简单的记录报错弹窗次数，避免重复报错
        Set<String> errorCount = new HashSet<>();

        FLAG:
        for (DatabaseColumnNode column : columns) {
            String typeName = column.getFieldType();
            for (TypeMapper typeMapper : typeMapperList) {
                try {
                    if (typeMapper.getMatchType() == MatchType.ORDINARY) {
                        if (typeName.equalsIgnoreCase(typeMapper.getColumnType())) {
                            continue FLAG;
                        }
                    } else {
                        // 不区分大小写的正则匹配模式
                        if (Pattern.compile(typeMapper.getColumnType(), Pattern.CASE_INSENSITIVE).matcher(typeName).matches()) {
                            continue FLAG;
                        }
                    }
                } catch (PatternSyntaxException e) {
                    if (!errorCount.contains(typeMapper.getColumnType())) {
                        Messages.showWarningDialog(
                                "类型映射《" + typeMapper.getColumnType() + "》存在语法错误，请及时修正。报错信息:" + e.getMessage(),
                                GlobalDict.TITLE_INFO);
                        errorCount.add(typeMapper.getColumnType());
                    }
                }
            }

//            return new GenerateTypeMatchDialog(project, dbTable).showAndGet();
            // 没找到类型，提示用户选择输入类型
//            boolean selectType = new Dialog(project, typeName).showAndGet();
//            if (!selectType) return false;
        }

        return true;
    }

    public static class Dialog extends DialogWrapper {

        private final String typeName;

        private JPanel mainPanel;

        private ComboBox<String> comboBox;

        protected Dialog(@Nullable Project project, String typeName) {
            super(project);
            this.typeName = typeName;
            this.initPanel();
        }

        private void initPanel() {
            setTitle(GlobalDict.TITLE_INFO);
            String msg = String.format("数据库类型%s，没有找到映射关系，请选择字段映射的类型", typeName);
            JLabel label = new JLabel(msg);
            this.mainPanel = new JPanel(new BorderLayout());
            this.mainPanel.setBorder(JBUI.Borders.empty(5, 10, 7, 10));
            mainPanel.add(label, BorderLayout.NORTH);
            this.comboBox = new ComboBox<>(FieldTypeMatch.DEFAULT_JAVA_TYPE_LIST);
            this.comboBox.setEditable(true);
            this.mainPanel.add(this.comboBox, BorderLayout.CENTER);
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            return this.mainPanel;
        }

        @Override
        protected void doOKAction() {
            super.doOKAction();
            String selectedItem = (String) this.comboBox.getSelectedItem();
            if (StringUtils.isEmpty(selectedItem)) {
                return;
            }

            TypeMapper typeMapper = new TypeMapper();
            typeMapper.setMatchType(MatchType.ORDINARY);
            typeMapper.setJavaType(selectedItem);
            typeMapper.setColumnType(typeName);
            CurrGroupUtils.getCurrTypeMapperGroup().getElementList().add(typeMapper);
        }
    }
}
