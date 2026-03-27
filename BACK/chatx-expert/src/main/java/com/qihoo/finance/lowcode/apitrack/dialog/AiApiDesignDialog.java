package com.qihoo.finance.lowcode.apitrack.dialog;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBUI;
import com.qihoo.finance.lowcode.apitrack.dialog.table.JsonTableWrap;
import com.qihoo.finance.lowcode.apitrack.dialog.table.ReqBodyTableWrap;
import com.qihoo.finance.lowcode.apitrack.dialog.table.ReqHeadersTableWrap;
import com.qihoo.finance.lowcode.apitrack.dialog.table.ReqQueryTableWrap;
import com.qihoo.finance.lowcode.apitrack.dialog.table.ReqUrlPathTableWrap;
import com.qihoo.finance.lowcode.apitrack.entity.ApiFormType;
import com.qihoo.finance.lowcode.apitrack.entity.ApiGroupNode;
import com.qihoo.finance.lowcode.apitrack.entity.ApiNode;
import com.qihoo.finance.lowcode.apitrack.entity.ApplicationNode;
import com.qihoo.finance.lowcode.apitrack.entity.RequestMethod;
import com.qihoo.finance.lowcode.apitrack.entity.ai.AiApiNode;
import com.qihoo.finance.lowcode.apitrack.entity.ai.AiMenuNode;
import com.qihoo.finance.lowcode.apitrack.util.ApiDesignUtils;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.constants.HttpMethod;
import com.qihoo.finance.lowcode.common.constants.OperateType;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.yapi.FormParam;
import com.qihoo.finance.lowcode.common.entity.dto.yapi.HeaderParam;
import com.qihoo.finance.lowcode.common.entity.dto.yapi.InterfaceDetailDTO;
import com.qihoo.finance.lowcode.common.entity.dto.yapi.InterfaceUpdateDTO;
import com.qihoo.finance.lowcode.common.entity.dto.yapi.PathParam;
import com.qihoo.finance.lowcode.common.entity.dto.yapi.QueryParam;
import com.qihoo.finance.lowcode.common.enums.LightVirtualType;
import com.qihoo.finance.lowcode.common.ui.base.DocumentUtils;
import com.qihoo.finance.lowcode.common.ui.base.VFlowLayout;
import com.qihoo.finance.lowcode.common.util.CacheManager;
import com.qihoo.finance.lowcode.common.util.DataContext;
import com.qihoo.finance.lowcode.common.util.EditorComponentUtils;
import com.qihoo.finance.lowcode.common.util.Icons;
import com.qihoo.finance.lowcode.common.util.JPanelUtils;
import com.qihoo.finance.lowcode.common.util.JTreeLoadingUtils;
import com.qihoo.finance.lowcode.common.util.NotifyUtils;
import com.qihoo.finance.lowcode.gentracker.dict.GlobalDict;
import com.qihoo.finance.lowcode.gentracker.tool.JSON;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jdesktop.swingx.JXComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.tree.TreeNode;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * GenerateApiDialog
 *
 * @author fengjinfu-jk
 * date 2023/8/31
 * @version 1.0.0
 * @apiNote GenerateApiDialog
 */
@Slf4j
public class AiApiDesignDialog extends DialogWrapper {
    public static ThreadLocal<Pair<Boolean, String>> validateSuccess = new ThreadLocal<>();
    private final OperateType operateType;
    private final Project project;
    private final AiApiNode apiNode;

    private static final ThreadLocal<Integer> FIELD_COUNT = new ThreadLocal<>();

    public static int addFieldCount() {
        Integer count = FIELD_COUNT.get();
        if (Objects.isNull(count)) {
            count = 0;
        }

        FIELD_COUNT.set(count + 1);
        return FIELD_COUNT.get();
    }

    private static final Map<String, Integer> bodyTabIndex = new HashMap<>() {{
        put("form", 0);
        put("json", 1);
        put("file", 2);
        put("raw", 3);
    }};


    private static final Map<String, Integer> responseTabIndex = new HashMap<>() {{
        put("json", 0);
        put("raw", 1);
    }};

    private static String getKeyFromValue(int index, Map<String, Integer> indexMap) {
        for (String key : indexMap.keySet()) {
            if (indexMap.get(key) == index) {
                return key;
            }
        }

        return null;
    }

    public AiApiDesignDialog(@Nullable Project project, OperateType operateType, AiApiNode apiNode) {
        super(project);
        this.project = project;
        this.operateType = operateType;
        this.apiNode = apiNode;

        // init components
        initComponents();
        // init data
        initComponentsData();
        // setting components size
        settingSizes();
        // setting components status
        settingComponentsStatus();

        // init data
        if (isNeedBindData()) bindInterfaceData();
        if (isView()) disableOkAction();

        log.info(Constants.Log.USER_ACTION, "用户打开接口设计");
    }

    private boolean isNeedBindData() {
        return isEdit() || isView() || isCopy();
    }

    private boolean isEdit() {
        return OperateType.isEdit(operateType);
    }

    private boolean isView() {
        return OperateType.isView(operateType);
    }

    private boolean isCopy() {
        return OperateType.isCopy(operateType);
    }

    private boolean isCreate() {
        return OperateType.isCreate(operateType);
    }

    @Override
    protected Action @NotNull [] createActions() {
        if (isCreate() || isEdit() || isCopy()) {
            return new Action[]{getOKAction(), getCancelAction()};
        }

        return new Action[]{getCancelAction()};
    }

    //------------------------------------------------------------------------------------------------------------------

    private void disableOkAction() {
        getOKAction().setEnabled(true);
    }

    private void settingComponentsStatus() {
        // apiMethodName 只允许输入英文
        ((AbstractDocument) apiMethodName.getDocument()).setDocumentFilter(DocumentUtils.createDocumentFilter(Constants.REGEX.ENG_NUM_UNDER_LINE));
    }

    private void bindInterfaceData() {
        new SwingWorker<InterfaceDetailDTO, InterfaceDetailDTO>(){

            @Override
            protected InterfaceDetailDTO doInBackground() throws Exception {
                return ApiDesignUtils.yapiInterfaceDetail(apiNode.getId());
            }

            @Override
            @SneakyThrows
            protected void done() {
                super.done();
                InterfaceDetailDTO apiDetail = get();
                if (Objects.isNull(apiDetail)) return;
                // base setting
                apiName.setText(apiDetail.getTitle());
                apiMethodName.setText(apiDetail.getMethodName());
                apiPathUrl.setText(apiDetail.getPath());
                apiMethod.setSelectedItem(apiDetail.getMethod().name());
                desc.setText(apiDetail.getDesc());

                // base setting
                List<PathParam> reqPath = apiDetail.getReqPath();
                if (CollectionUtils.isNotEmpty(reqPath)) {
                    DefaultTableModel pathTableModel = (DefaultTableModel) pathTable.getModel();
                    for (PathParam pathParam : reqPath) {
                        // "路由参数名称", "参数示例", "备注"
                        if (pathTableModel.getRowCount() > 0) {
                            pathTableModel.removeRow(0);
                        }

                        pathTableModel.addRow(new Object[]{pathParam.getName(), pathParam.getExample(), pathParam.getDesc()});
                    }
                }

                // request param Body
                String bodyType = apiDetail.getReqBodyType();
                if (StringUtils.isNotEmpty(bodyType)) {
                    // 定位
                    requestTabbPanel.setSelectedIndex(0);
                    bodyTab.setSelectedIndex(bodyTabIndex.getOrDefault(bodyType.toLowerCase(), 0));

                    // 填充body数据
                    if (ApiFormType.Form.name().equalsIgnoreCase(bodyType)) {
                        List<FormParam> formBody = apiDetail.getReqForm();
                        if (CollectionUtils.isNotEmpty(formBody)) {
                            DefaultTableModel formBodyModel = (DefaultTableModel) formTable.getModel();
                            for (FormParam formParam : formBody) {
                                // "参数名称", "类型", "必须", "参数示例", "备注", "操作"
                                formBodyModel.addRow(new Object[]{formParam.getName(), formParam.getType(), formParam.getRequired(), formParam.getExample(), formParam.getDesc()});
                            }
                        }
                    }
                    if (ApiFormType.Json.name().equalsIgnoreCase(bodyType)) {
                        EditorComponentUtils.write(project, reqBodyJsonTextArea, JSON.formatJson(apiDetail.getReqBody()), false);
                        reqBodyJsonTableWrap.setStructureJson(apiDetail.getReqBody());

                        Object[][] editTableData = reqBodyJsonTableWrap.getEditTableData();
                        DefaultTableModel model = (DefaultTableModel) bodyJsonTable.getModel();
                        for (Object[] editTableDatum : editTableData) {
                            model.addRow(editTableDatum);
                        }
                    }
                    if (ApiFormType.File.name().equalsIgnoreCase(bodyType)) {
                        EditorComponentUtils.write(project, reqBodyFileTextArea, JSON.formatJson(apiDetail.getReqBody()), false);
                    }
                    if (ApiFormType.Raw.name().equalsIgnoreCase(bodyType)) {
                        EditorComponentUtils.write(project, reqBodyRawTextArea, JSON.formatJson(apiDetail.getReqBody()), false);
                    }
                }

                // request param Query
                List<QueryParam> reqQuery = apiDetail.getReqQuery();
                if (CollectionUtils.isNotEmpty(reqQuery)) {
                    DefaultTableModel reqQueryModel = (DefaultTableModel) queryTable.getModel();
                    for (QueryParam queryParam : reqQuery) {
                        reqQueryModel.addRow(new Object[]{queryParam.getName(), queryParam.getRequired(), queryParam.getExample(), queryParam.getDesc()});
                    }

                    if (StringUtils.isEmpty(bodyType)) {
                        requestTabbPanel.setSelectedIndex(Math.min(requestTabbPanel.getTabCount() - 1, 1));
                    }
                }

                // request param Headers
                List<HeaderParam> reqHeaders = apiDetail.getReqHeaders();
                if (CollectionUtils.isNotEmpty(reqHeaders)) {
                    DefaultTableModel reqHeadersModel = (DefaultTableModel) headersTable.getModel();
                    for (HeaderParam headerParam : reqHeaders) {
                        reqHeadersModel.addRow(new Object[]{headerParam.getName(), headerParam.getValue(), headerParam.getExample(), headerParam.getDesc()});
                    }
                }

                // response param JSON
                String resBodyType = apiDetail.getResBodyType();
                if (Objects.nonNull(resBodyType)) {
                    responseTab.setSelectedIndex(Math.min(responseTabIndex.getOrDefault(resBodyType.toLowerCase(), 0), responseTab.getTabCount() - 1));
                    if (ApiFormType.Json.name().equalsIgnoreCase(resBodyType)) {
                        EditorComponentUtils.write(project, resJsonTextArea, JSON.formatJson(apiDetail.getResBody()), false);
                        resBodyJsonTableWrap.setStructureJson(apiDetail.getResBody());

                        Object[][] editTableData = resBodyJsonTableWrap.getEditTableData();
                        DefaultTableModel model = (DefaultTableModel) resJsonTable.getModel();
                        for (Object[] editTableDatum : editTableData) {
                            model.addRow(editTableDatum);
                        }
                    }
                    if (ApiFormType.Raw.name().equalsIgnoreCase(resBodyType)) {
                        EditorComponentUtils.write(project, resRawTextArea, JSON.formatJson(apiDetail.getResBody()), false);
                    }
                }

                // json preview
                resJsonTabPanel.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        Component selectedComponent = resJsonTabPanel.getSelectedComponent();
                        if (selectedComponent.equals(resJsonPreviewPanel)) {
                            // 更新JSON预览
                            String json = resBodyJsonTableWrap.getDataJsonPreview(resJsonTable);
                            EditorComponentUtils.write(project, resJsonPreviewArea, json);
                        }
                    }
                });
            }
        }.execute();
    }

    private void superDoOKAction() {
        super.doOKAction();
        super.setOKActionEnabled(true);
    }

    @Override
    protected void doOKAction() {
        log.info(Constants.Log.USER_ACTION, "用户提交接口设计");

        enabledDialogSelector(false);
        SwingWorker<Result<?>, Result<?>> swingWorker = new SwingWorker<>() {
            boolean success = false;
            Result<?> result;

            @Override
            protected Result<?> doInBackground() {
                progressBar.setVisible(true);
                try {
                    AiApiDesignDialog.validateSuccess.set(Pair.of(true, StringUtils.EMPTY));
                    result = onOK();
                    success = true;
                } catch (Exception e) {
                    success = false;
                    log.error(ExceptionUtils.getStackTrace(e));
                } finally {
                    progressBar.setVisible(false);
                    enabledDialogSelector(true);
                    AiApiDesignDialog.validateSuccess.remove();
                }

                return result;
            }

            @Override
            protected void done() {
                if (success && Objects.nonNull(result)) {
                    if (result.isSuccess()) {
                        // 气泡提醒, 在NotificationGroup中添加消息通知内容，以及消息类型。这里为MessageType.INFO
                        NotifyUtils.notify(isEdit() ? "接口更新成功" : "接口创建成功", NotificationType.INFORMATION);

                        // 缓存需要刷新
                        CacheManager.refreshInnerCache();
                        JTree apiTree = DataContext.getInstance(project).getApiTree();
                        ApiGroupNode apiGroupNode = DataContext.getInstance(project).getSelectApiGroupNode();
                        JTreeLoadingUtils.loading(false, apiTree, apiGroupNode, () -> ApiDesignUtils.apiInterfaceList(apiGroupNode));

                        // 打开代码生成界面
                        SwingUtilities.invokeLater(() -> {
                            new ApiGenCodeDialog(project).show();
                            superDoOKAction();
                        });

                        Messages.showInfoMessage((isEdit() ? "接口更新成功" : "接口创建成功") + ", 请进行代码生成", GlobalDict.TITLE_INFO);
                    } else {
                        Messages.showMessageDialog(result.getErrorMsg(), isEdit() ? "更新失败" : "创建失败", Icons.scaleToWidth(Icons.FAIL, 60));
                    }
                }

                super.done();
            }
        };

        swingWorker.execute();
    }

    private Result<?> onOK() {
        InterfaceUpdateDTO itfDTO = new InterfaceUpdateDTO();
        DataContext dataContext = DataContext.getInstance(project);

        // 1.基本设置
        // 基本设置-接口名称
        itfDTO.setTitle(this.apiName.getText());
        // 基本设置-接口分类
        Object selectGroup = this.apiGroup.getSelectedItem();
        ApiGroupNode apiGroup = Objects.nonNull(selectGroup) ? (ApiGroupNode) selectGroup : dataContext.getSelectApiGroupNode();
        itfDTO.setCategoryId(Long.parseLong(apiGroup.getId()));
        itfDTO.setToken(apiGroup.getProjectToken());
        ApplicationNode applicationNode = dataContext.getSelectApplicationNode();
        if (Objects.nonNull(applicationNode)) {
            itfDTO.setAppCode(applicationNode.getAppCode());
            itfDTO.setProjectId(applicationNode.getProjectId());
        }

        // 基本设置-接口路径
        itfDTO.setMethod(HttpMethod.valueOf((String) this.apiMethod.getSelectedItem()));
        itfDTO.setPath(this.apiPathUrl.getText());

        // 基本设置-路径path
        setReqPath(itfDTO);

        // 2.请求参数设置
        // 请求参数设置-Body
        itfDTO.setReqBodyType(getKeyFromValue(this.bodyTab.getSelectedIndex(), bodyTabIndex));
        setReqBody(itfDTO);

        // 请求参数设置-Query
        itfDTO.setReqQuery(ReqQueryTableWrap.getReqQuery(this.queryTable));

        // 请求参数设置-Headers
        itfDTO.setReqHeaders(ReqHeadersTableWrap.getReqHeaders(this.headersTable));

        // 3.返回参数设置
        itfDTO.setResBodyType(getKeyFromValue(this.responseTab.getSelectedIndex(), responseTabIndex));
        setResBody(itfDTO);

        Boolean validate = AiApiDesignDialog.validateSuccess.get().getKey();
        if (!validate) {
            Result<Object> fail = new Result<>();
            fail.setErrorMsg(AiApiDesignDialog.validateSuccess.get().getValue());
            fail.setSuccess(false);
            return fail;
        }

        if (isEdit()) {
            ApiNode selectApiNode = DataContext.getInstance(project).getSelectApiNode();
            itfDTO.setId(selectApiNode.getId());
            return ApiDesignUtils.apiInterfaceUpdate(itfDTO);
        } else {
            return ApiDesignUtils.apiInterfaceAdd(itfDTO);
        }
    }

    private void setReqPath(InterfaceUpdateDTO itfDTO) {
        if (this.pathTable.getRowCount() > 0) {
            itfDTO.setReqPath(ReqUrlPathTableWrap.getReqPath(this.pathTable));
        }
    }

    private void setResBody(InterfaceUpdateDTO itfDTO) {
        int selectedIndex = this.responseTab.getSelectedIndex();
        switch (selectedIndex) {
            case 0:
                // json
                itfDTO.setResBody(JsonTableWrap.getRawJson(this.resJsonTable));
                break;
            case 1:
                // raw
                itfDTO.setResBody(this.resRawTextArea.getDocument().getText());
                break;
            default:
        }
    }

    private void setReqBody(InterfaceUpdateDTO itfDTO) {
        int selectedIndex = this.bodyTab.getSelectedIndex();
        switch (selectedIndex) {
            case 0:
                // form
                itfDTO.setReqForm(ReqBodyTableWrap.getReqForm(this.formTable));
                break;
            case 1:
                // json
                itfDTO.setReqBody(JsonTableWrap.getRawJson(this.bodyJsonTable));
                itfDTO.setReqForm(new ArrayList<>());
                break;
            case 2:
                // file
                itfDTO.setReqBody(this.reqBodyFileTextArea.getDocument().getText());
                itfDTO.setReqForm(new ArrayList<>());
                break;
            case 3:
                // raw
                itfDTO.setReqBody(this.reqBodyRawTextArea.getDocument().getText());
                itfDTO.setReqForm(new ArrayList<>());
                break;
            default:
        }
    }

    private void enabledDialogSelector(boolean enable) {
        this.setOKActionEnabled(enable);
    }

    private void initComponentsData() {
        // apiGroup
        Enumeration<? extends TreeNode> menus = apiNode.getParent().getParent().children();
        while (menus.hasMoreElements()) {
            AiMenuNode menu = (AiMenuNode)menus.nextElement();
            apiGroup.addItem(menu);
            if (menu.getId().equals(((AiMenuNode)apiNode.getParent()).getId())) {
                apiGroup.setSelectedItem(menu);
            }
        }

        // apiMethod
        for (RequestMethod value : RequestMethod.values()) {
            apiMethod.addItem(value.name());
        }

        importReqJsonButton.addActionListener(e -> {
            // 导入Json
            new ImportJsonDialog(project, this.reqBodyJsonTableWrap, this.bodyJsonTable).showAndGet();
        });

        importResJsonButton.addActionListener(e -> {
            // 导入Json
            new ImportJsonDialog(project, this.resBodyJsonTableWrap, this.resJsonTable).showAndGet();
        });
    }

    private void settingSizes() {
        JPanelUtils.setSize(dialogPane, new Dimension(900, 600));

        formTableScroll.setPreferredSize(new Dimension(-1, 400));
        queryTableScroll.setPreferredSize(new Dimension(-1, 400));
        headersTableScroll.setPreferredSize(new Dimension(-1, 400));
        bodyJsonScroll.setPreferredSize(new Dimension(-1, 400));
        reqBodyFileScroll.setPreferredSize(new Dimension(-1, 400));
        reqBodyRawScroll.setPreferredSize(new Dimension(-1, 400));

        resJsonTableScroll.setPreferredSize(new Dimension(-1, 400));
        previewScroll.setPreferredSize(new Dimension(-1, 400));
        resRawScroll.setPreferredSize(new Dimension(-1, 400));

        label7.setFont(new Font("微软雅黑", Font.BOLD, 16));
        label7.setHorizontalAlignment(SwingConstants.CENTER);
        label8.setFont(new Font("微软雅黑", Font.BOLD, 16));
        label8.setHorizontalAlignment(SwingConstants.CENTER);
        label9.setFont(new Font("微软雅黑", Font.BOLD, 16));
        label9.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        dialogPane = new JPanel();
        JBScrollPane mainScroll = new JBScrollPane();
        JPanel mainPanel = new JPanel();
        label7 = new JLabel();
        JPanel contentPanel = new JPanel();
        JLabel label1 = new JLabel();
        apiName = new JTextField();
        JLabel apiMethodNameLabel = new JLabel();
        apiMethodName = new JTextField();

        JLabel apiGroupLabel = new JLabel();
        apiGroup = new JXComboBox();

        JPanel progressPanel = new JPanel();

        JLabel label4 = new JLabel();
        apiMethod = new JXComboBox();
        apiMethod.addActionListener(e -> {
            Object method = apiMethod.getSelectedItem();
            boolean bodyTab = RequestMethod.POST.name().equals(method)
                    || RequestMethod.PUT.name().equals(method)
                    ||RequestMethod.DELETE.name().equals(method)
                    || RequestMethod.PATCH.name().equals(method);

            // 按需移除或添加body页
            Component componentAt = requestTabbPanel.getComponentAt(0);
            if(bodyTab && (!componentAt.equals(bodyTabPanel))) {
                requestTabbPanel.insertTab("Body", null, bodyTabPanel, null, 0);
                requestTabbPanel.setSelectedIndex(0);
            } else if(!bodyTab && componentAt.equals(bodyTabPanel)){
                requestTabbPanel.remove(bodyTabPanel);
            }
        });

        JBScrollPane pathTableScroll = new JBScrollPane(pathTable);
        pathTableScroll.setVisible(false);
        ReqUrlPathTableWrap reqUrlPathTable = new ReqUrlPathTableWrap(isNeedBindData(), dialogPane);
        pathTable = reqUrlPathTable.createTable();

        apiPathUrl = new JTextField();
        apiPathUrl.getDocument().addDocumentListener(reqUrlPathTable.updateApiPathUrl(pathTableScroll, pathTable, apiPathUrl));

        JSeparator separator3 = new JSeparator();
        JPanel paramPanel = new JPanel();
        label8 = new JLabel();
        requestTabbPanel = new JBTabbedPane();
        bodyTabPanel = new JPanel();
        JBScrollPane bodyTabScrollPane = new JBScrollPane();

        bodyTab = new JBTabbedPane();
        JPanel formTabPanel = new JPanel();

        formTableScroll = new JBScrollPane();

        ReqBodyTableWrap reqBodyFormTableWrap = new ReqBodyTableWrap(isNeedBindData());
        formTable = reqBodyFormTableWrap.createTable();

        JButton addFormButton = new JButton();
        addFormButton.addActionListener(e -> reqBodyFormTableWrap.addRow(e, formTable));

        JPanel bodyJsonPanel = new JPanel();
        importReqJsonButton = new JButton();
        bodyJsonScroll = new JBScrollPane();

        reqBodyJsonTextArea = EditorComponentUtils.createEditorPanel(project, LightVirtualType.JSON);
        reqBodyJsonTableWrap = new JsonTableWrap(isNeedBindData());
        bodyJsonTable = reqBodyJsonTableWrap.createTable();

        JButton addReqJsonParamButton = new JButton();
        addReqJsonParamButton.addActionListener(e -> reqBodyJsonTableWrap.addRow(e, bodyJsonTable));
        JPanel panel6 = new JPanel();
        reqBodyFileScroll = new JBScrollPane();

        reqBodyFileTextArea = EditorComponentUtils.createEditorPanel(project, LightVirtualType.JSON);
        JPanel panel7 = new JPanel();
        reqBodyRawScroll = new JBScrollPane();

        reqBodyRawTextArea = EditorComponentUtils.createEditorPanel(project, LightVirtualType.JSON);
        JPanel panel9 = new JPanel();

        queryTableScroll = new JBScrollPane();

        ReqQueryTableWrap reqQueryTable = new ReqQueryTableWrap(isNeedBindData());
        queryTable = reqQueryTable.createTable();

        JButton addQueryButton = new JButton();
        addQueryButton.addActionListener(e -> reqQueryTable.addRow(e, queryTable));

        JPanel panel10 = new JPanel();

        headersTableScroll = new JBScrollPane();

        ReqHeadersTableWrap headersTableWrap = new ReqHeadersTableWrap(isNeedBindData());
        headersTable = headersTableWrap.createTable();

        JButton addHeaderButton = new JButton();
        addHeaderButton.addActionListener(e -> headersTableWrap.addRow(e, headersTable));

        JSeparator separator2 = new JSeparator();
        label9 = new JLabel();
        JBScrollPane scrollPane3 = new JBScrollPane();
        responseTab = new JBTabbedPane();


        importResJsonButton = new JButton();
        resBodyJsonTableWrap = new JsonTableWrap(isNeedBindData());
        resJsonTable = resBodyJsonTableWrap.createTable();

        JButton addResJsonParamButton = new JButton();
        addResJsonParamButton.addActionListener(e -> resBodyJsonTableWrap.addRow(e, resJsonTable));

        JPanel jsonResponsePanel = new JPanel();
        resJsonTabPanel = new JBTabbedPane();
        JPanel resJsonTemplatePanel = new JPanel();
        resJsonTextArea = EditorComponentUtils.createEditorPanel(project, LightVirtualType.JSON);

        resJsonPreviewPanel = new JPanel();
        previewScroll = new JBScrollPane();

        resJsonTableScroll = new JBScrollPane();

        resJsonPreviewArea = EditorComponentUtils.createEditorPanel(project, LightVirtualType.JSON);

        JPanel rawResponsePanel = new JPanel();
        resRawScroll = new JBScrollPane();
        resRawTextArea = EditorComponentUtils.createEditorPanel(project, LightVirtualType.JSON);
        JSeparator separator1 = new JSeparator();
        descLabel = new JLabel();
        desc = new JTextField();

        progressBar = new JProgressBar();

        //======== this ========
//        setTitle("API设置");
//        var contentPane = getContentPane();
//        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(JBUI.Borders.empty(12));
            dialogPane.setLayout(new BorderLayout());

            //======== scrollPane14 ========
            {

                //======== panel2 ========
                {
                    mainPanel.setLayout(new GridLayoutManager(4, 2, JBUI.insets(10), -1, -1));

                    //---- label7 ----
                    label7.setText("基本设置");
                    mainPanel.add(label7, new GridConstraints(0, 0, 1, 1,
                        GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        null, null, null));

                    //======== contentPanel ========
                    {
                        contentPanel.setLayout(new GridLayoutManager(50, 3, JBUI.emptyInsets(), -1, -1));

                        //---- label1 ----
                        label1.setText("        接口名称");
                        contentPanel.add(label1, new GridConstraints(0, 0, 1, 1,
                            GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null, null, null));
                        contentPanel.add(apiName, new GridConstraints(0, 1, 1, 2,
                            GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null, null, null));

                        //---- descLabel ----
                        descLabel.setText("        接口描述");
                        contentPanel.add(descLabel, new GridConstraints(1, 0, 1, 1,
                                GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                null, null, null));
                        contentPanel.add(desc, new GridConstraints(1, 1, 1, 2,
                                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                null, null, null));

                        //---- label2 ----
                        apiGroupLabel.setText("        接口分类");
                        contentPanel.add(apiGroupLabel, new GridConstraints(4, 0, 1, 1,
                            GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null, null, null));
                        contentPanel.add(apiGroup, new GridConstraints(4, 1, 1, 2,
                            GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null, null, null));

                        //---- label4 ----
                        label4.setText("        接口路径");
                        contentPanel.add(label4, new GridConstraints(6, 0, 1, 1,
                            GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null, null, null));
                        contentPanel.add(apiMethod, new GridConstraints(6, 1, 1, 1,
                            GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null, null, null));
                        contentPanel.add(apiPathUrl, new GridConstraints(6, 2, 1, 1,
                            GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null, null, null));

                        //======== pathTableScrollPane ========
                        {
                            pathTableScroll.setViewportView(pathTable);
                        }
                        contentPanel.add(pathTableScroll, new GridConstraints(8, 1, 1, 2,
                            GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null, null, null));

                        //---- label5 ----
//                        label5.setText("        Tag");
//                        contentPanel.add(label5, new GridConstraints(4, 0, 1, 1,
//                            GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
//                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
//                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
//                            null, null, null));
//                        contentPanel.add(tag, new GridConstraints(4, 1, 1, 1,
//                            GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
//                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
//                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
//                            null, null, null));
//
//                        //---- button1 ----
//                        button1.setText("Tag设置");
//                        contentPanel.add(button1, new GridConstraints(4, 2, 1, 1,
//                            GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
//                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
//                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
//                            null, null, null));
//
//                        //---- label6 ----
//                        label6.setText("        状态");
//                        contentPanel.add(label6, new GridConstraints(5, 0, 1, 1,
//                            GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
//                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
//                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
//                            null, null, null));
//                        contentPanel.add(status, new GridConstraints(5, 1, 1, 1,
//                            GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
//                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
//                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
//                            null, null, null));
                    }
                    mainPanel.add(contentPanel, new GridConstraints(1, 0, 1, 1,
                        GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        null, null, null));
                    mainPanel.add(separator3, new GridConstraints(2, 0, 1, 1,
                        GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        null, null, null));

                    //======== panel3 ========
                    {
                        paramPanel.setLayout(new GridLayoutManager(10, 2, JBUI.emptyInsets(), -1, -1));

                        //---- label8 ----
                        label8.setText("请求参数设置");
                        paramPanel.add(label8, new GridConstraints(0, 0, 1, 1,
                            GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null, null, null));

                        //======== tabbedPane3 ========
                        {

                            //======== panel8 ========
                            {
                                bodyTabPanel.setLayout(new GridLayoutManager(3, 2, JBUI.emptyInsets(), -1, -1));

                                //======== scrollPane2 ========
                                {

                                    //======== tabbedPane1 ========
                                    {

                                        //======== panel4 ========
                                        {
                                            formTabPanel.setLayout(new VFlowLayout());

                                            //---- button4 ----
                                            addFormButton.setText("添加form参数");
                                            formTabPanel.add(addFormButton, VFlowLayout.TOP);

                                            //======== formTableScrollPane ========
                                            {
                                                formTableScroll.setViewportView(formTable);
                                            }
                                            formTabPanel.add(formTableScroll);
                                        }
                                        bodyTab.addTab("form", formTabPanel);

                                        //======== panel5 ========
                                        {
//                                            requestJsonPanel.setLayout(new GridLayoutManager(3, 2, JBUI.emptyInsets(), -1, -1));
                                            bodyJsonPanel.setLayout(new VFlowLayout());
                                            importReqJsonButton.setText("导入Json");
                                            addReqJsonParamButton.setText("添加参数");
                                            JPanel reqJsonBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                                            reqJsonBtnPanel.add(importReqJsonButton);
                                            reqJsonBtnPanel.add(addReqJsonParamButton);
                                            bodyJsonPanel.add(reqJsonBtnPanel, VFlowLayout.TOP);
                                            {
//                                                bodyJsonScrollPane.setViewportView(reqBodyJsonTextArea);
                                                bodyJsonScroll.setViewportView(bodyJsonTable);
                                            }
                                            //---- button5 ----
                                            bodyJsonPanel.add(bodyJsonScroll, new GridConstraints(0, 0, 1, 1,
                                                GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_BOTH,
                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                null, null, null));
                                        }
                                        bodyTab.addTab("json", bodyJsonPanel);

                                        //======== panel6 ========
                                        {
                                            panel6.setLayout(new GridLayoutManager(3, 2, JBUI.emptyInsets(), -1, -1));

                                            //======== scrollPane4 ========
                                            {
                                                reqBodyFileScroll.setViewportView(reqBodyFileTextArea.getComponent());
                                            }
                                            panel6.add(reqBodyFileScroll, new GridConstraints(0, 0, 1, 1,
                                                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                null, null, null));
                                        }
                                        bodyTab.addTab("file", panel6);

                                        //======== panel7 ========
                                        {
                                            panel7.setLayout(new GridLayoutManager(3, 2, JBUI.emptyInsets(), -1, -1));

                                            //======== scrollPane5 ========
                                            {
                                                reqBodyRawScroll.setViewportView(reqBodyRawTextArea.getComponent());
                                            }
                                            panel7.add(reqBodyRawScroll, new GridConstraints(0, 0, 1, 1,
                                                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                null, null, null));
                                        }
                                        bodyTab.addTab("raw", panel7);
                                    }
                                    bodyTabScrollPane.setViewportView(bodyTab);
                                }
                                bodyTabPanel.add(bodyTabScrollPane, new GridConstraints(0, 0, 1, 1,
                                    GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                    GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                    null, null, null));
                            }
                            requestTabbPanel.addTab("Body", bodyTabPanel);

                            //======== panel9 ========
                            {
                                panel9.setLayout(new VFlowLayout());

                                //---- button2 ----
                                addQueryButton.setText("添加Query参数");
                                panel9.add(addQueryButton, VFlowLayout.TOP);

                                //======== scrollPane11 ========
                                {
                                    queryTableScroll.setViewportView(queryTable);
                                }
                                panel9.add(queryTableScroll);
                            }
                            requestTabbPanel.addTab("Query", panel9);

                            //======== panel10 ========
                            {
                                panel10.setLayout(new VFlowLayout());

                                //---- button3 ----
                                addHeaderButton.setText("添加Header");
                                panel10.add(addHeaderButton, VFlowLayout.TOP);

                                //======== scrollPane12 ========
                                {
                                    headersTableScroll.setViewportView(headersTable);
                                }
                                panel10.add(headersTableScroll);
                            }
                            requestTabbPanel.addTab("Headers", panel10);
                        }
                        paramPanel.add(requestTabbPanel, new GridConstraints(1, 0, 1, 1,
                            GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null, null, null));
                        paramPanel.add(separator2, new GridConstraints(3, 0, 1, 1,
                            GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null, null, null));

                        //---- label9 ----
                        label9.setText("返回参数设置");
                        paramPanel.add(label9, new GridConstraints(4, 0, 1, 1,
                            GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null, null, null));

                        //======== scrollPane3 ========
                        {

                            //======== tabbedPane2 ========
                            {

                                //======== panel11 ========
                                {
                                    jsonResponsePanel.setLayout(new GridLayoutManager(3, 2, JBUI.emptyInsets(), -1, -1));

                                    //======== tabbedPane4 ========
                                    {

                                        //======== panel13 ========
                                        {
                                            resJsonTemplatePanel.setLayout(new VFlowLayout());
                                            JPanel resBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                                            importResJsonButton.setText("导入Json");
                                            addResJsonParamButton.setText("添加参数");

                                            resBtnPanel.add(importResJsonButton);
                                            resBtnPanel.add(addResJsonParamButton);
                                            resJsonTemplatePanel.add(resBtnPanel, VFlowLayout.TOP);
                                            {
                                                resJsonTableScroll.setViewportView(resJsonTable);
                                            }
                                            //---- button6 ----
                                            resJsonTemplatePanel.add(resJsonTableScroll, new GridConstraints(0, 0, 1, 1,
                                                GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_BOTH,
                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                null, null, null));
                                        }
                                        resJsonTabPanel.addTab("模板", resJsonTemplatePanel);

                                        //======== panel14 ========
                                        {
                                            resJsonPreviewPanel.setLayout(new GridLayoutManager(3, 2, JBUI.emptyInsets(), -1, -1));

                                            //======== scrollPane7 ========
                                            {
                                                previewScroll.setViewportView(resJsonPreviewArea.getComponent());
                                            }
                                            resJsonPreviewPanel.add(previewScroll, new GridConstraints(0, 0, 1, 1,
                                                GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                                null, null, null));
                                        }
                                        resJsonTabPanel.addTab("预览", resJsonPreviewPanel);
                                    }
//                                    {
//                                        scrollPane07.setViewportView(resJsonTextArea);
//                                    }
//                                    jsonResponsePanel.add(scrollPane07, new GridConstraints(0, 0, 1, 1,
                                    jsonResponsePanel.add(resJsonTabPanel, new GridConstraints(0, 0, 1, 1,
                                        GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                        null, null, null));
                                }
                                responseTab.addTab("JSON", jsonResponsePanel);

                                //======== panel12 ========
                                {
                                    rawResponsePanel.setLayout(new GridLayoutManager(3, 2, JBUI.emptyInsets(), -1, -1));

                                    //======== scrollPane6 ========
                                    {
                                        resRawScroll.setViewportView(resRawTextArea.getComponent());
                                    }
                                    rawResponsePanel.add(resRawScroll, new GridConstraints(0, 0, 1, 1,
                                        GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                                        null, null, null));
                                }
                                responseTab.addTab("RAW", rawResponsePanel);
                            }
                            scrollPane3.setViewportView(responseTab);
                        }
                        paramPanel.add(scrollPane3, new GridConstraints(5, 0, 1, 1,
                            GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null, null, null));
                        paramPanel.add(separator1, new GridConstraints(6, 0, 1, 1,
                            GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                            null, null, null));
                    }
                    mainPanel.add(paramPanel, new GridConstraints(3, 0, 1, 1,
                        GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
                        null, null, null));
                }
                mainScroll.setViewportView(mainPanel);
            }

            mainScroll.getVerticalScrollBar().setUnitIncrement(20);
            dialogPane.add(mainScroll, BorderLayout.CENTER);

            //======== progressBar ========
            {
                progressBar.setStringPainted(true);
                // 设置采用不确定进度条
                progressBar.setIndeterminate(true);
                progressBar.setVisible(false);
                progressBar.setString("接口保存中......");// 设置提示信息

                progressPanel.setBorder(JBUI.Borders.emptyTop(12));
                progressPanel.setLayout(new BorderLayout());
                progressPanel.add(progressBar, BorderLayout.CENTER);
            }
            dialogPane.add(progressPanel, BorderLayout.PAGE_END);

        }

        init();
        setTitle(GlobalDict.TITLE_INFO + (isEdit() ? "-编辑接口" : isView() ? "-接口详情" : "-新增接口"));
        setOKButtonText("保存");
        setCancelButtonText("取消");
        this.setModal(false);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel dialogPane;
    private JLabel label7;
    private JTextField apiName;
    private JTextField apiMethodName;
    private JXComboBox apiGroup;
    private JXComboBox apiMethod;
    private JTextField apiPathUrl;
    private JBTable pathTable;
    private JLabel label8;
    private JBTabbedPane requestTabbPanel;
    private JPanel bodyTabPanel;
    private JBTabbedPane bodyTab;
    private JBScrollPane formTableScroll;
    private JBTable formTable;
    private JButton importReqJsonButton;
    private JBScrollPane bodyJsonScroll;
    private JBTable bodyJsonTable;
    private Editor reqBodyJsonTextArea;
    private JsonTableWrap reqBodyJsonTableWrap;
    private JBScrollPane reqBodyFileScroll;
    private Editor reqBodyFileTextArea;
    private JBScrollPane reqBodyRawScroll;
    private Editor reqBodyRawTextArea;
    private JBScrollPane queryTableScroll;
    private JBTable queryTable;
    private JBScrollPane headersTableScroll;
    private JBTable headersTable;
    private JLabel label9;
    private JBTabbedPane responseTab;
    private JBTabbedPane resJsonTabPanel;
    private JsonTableWrap resBodyJsonTableWrap;
    private Editor resJsonTextArea;
    private JButton importResJsonButton;
    private JBTable resJsonTable;
    private JPanel resJsonPreviewPanel;
    private JBScrollPane previewScroll;
    private JBScrollPane resJsonTableScroll;
    private Editor resJsonPreviewArea;
    private JBScrollPane resRawScroll;
    private Editor resRawTextArea;
    private JLabel descLabel;
    private JTextField desc;
    private JProgressBar progressBar;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return dialogPane;
    }

    @Override
    protected void dispose() {
        FIELD_COUNT.remove();
        super.dispose();
    }
}
