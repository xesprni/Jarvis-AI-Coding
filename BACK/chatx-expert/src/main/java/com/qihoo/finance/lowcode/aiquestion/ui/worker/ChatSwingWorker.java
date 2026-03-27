package com.qihoo.finance.lowcode.aiquestion.ui.worker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.qihoo.finance.lowcode.aiquestion.ui.QuestionPanel;
import com.qihoo.finance.lowcode.aiquestion.ui.component.CancelableButton;
import com.qihoo.finance.lowcode.aiquestion.ui.factory.InputPanelFactory;
import com.qihoo.finance.lowcode.aiquestion.ui.factory.stream.StreamMessageRowFactory;
import com.qihoo.finance.lowcode.common.action.MethodAskAIAction;
import com.qihoo.finance.lowcode.common.constants.Constants;
import com.qihoo.finance.lowcode.common.constants.QuestionType;
import com.qihoo.finance.lowcode.common.constants.ServiceErrorCode;
import com.qihoo.finance.lowcode.common.entity.FileUpload;
import com.qihoo.finance.lowcode.common.entity.SimpleMethodInfo;
import com.qihoo.finance.lowcode.common.entity.base.Result;
import com.qihoo.finance.lowcode.common.entity.dto.askai.AssistantDetail;
import com.qihoo.finance.lowcode.common.entity.dto.askai.AssistantInfo;
import com.qihoo.finance.lowcode.common.entity.dto.askai.ChatCompletionRequest;
import com.qihoo.finance.lowcode.common.entity.dto.askai.ChatCompletionResponse;
import com.qihoo.finance.lowcode.common.entity.dto.plugins.DatasetInfo;
import com.qihoo.finance.lowcode.common.util.*;
import com.qihoo.finance.lowcode.common.utils.JSON;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import com.qihoo.finance.lowcode.settings.ui.Option;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

@Slf4j
public class ChatSwingWorker extends SwingWorker<String, String> {

    private static final Logger LOG = Logger.getInstance(ChatSwingWorker.class);
    public final static int READ_TIME_OUT = 60 * 1000;

    private final Project project;
    private final JPanel viewPanel;
    private final JBScrollPane viewScrollPane;
    private final QuestionPanel questionPanel;
    private final String assistant;
    private final QuestionType questionType;
    private String question;
    private String stackTrace;

//    @Setter
    private StreamMessageRowFactory streamMessageRowFactory;
    public void setStreamMessageRowFactory(StreamMessageRowFactory streamMessageRowFactory) {
        this.streamMessageRowFactory = streamMessageRowFactory;
    }

    private Long logId = 0L;
    private String messageId = StringUtils.EMPTY;
    private JLabel reAnswerOrStop;

    public ChatSwingWorker(Project project, QuestionPanel questionPanel, String assistant, QuestionType questionType,
                           String question, String stackTrace) {
        this.project = project;
        this.questionPanel = questionPanel;
        this.viewPanel = questionPanel.getViewPanel();
        this.viewScrollPane = questionPanel.getViewScrollPane();
        this.assistant = assistant;
        this.questionType = questionType;
        this.question = question;
        this.stackTrace = stackTrace;
    }

    @Override
    protected String doInBackground() throws Exception {
        String defaultMsg = ChatxApplicationSettings.settings().chatErrorMsg;
        String finalMsg = null;
        StringBuilder builder = new StringBuilder();
        InputPanelFactory input = questionPanel.getInputPanelFactory();
        try {
            String conversationId = null;
            if (questionType == QuestionType.ASK) {
                conversationId = input.getConversationId();
            }
            // 获取http connection
            HttpURLConnection conn = getHttpURLConnection();
            // 组装请求参数
            ChatCompletionRequest request = ChatCompletionRequest.of(conversationId, assistant, questionType, question);
            AssistantInfo assistantInfo = ChatxApplicationSettings.settings().getAssistantInfo(assistant);
            if (Objects.nonNull(assistantInfo.getCustomParam()) && MapUtils.isNotEmpty(assistantInfo.getCustomParam().getValues())) {
                Option<String> selectedItem = (Option<String>) input.getAssistantParam().getSelectedItem();
                request.addCustomParam(assistantInfo.getCustomParam().getKey(), selectedItem.getValue());
            }
            if (Constants.DEFAULT_ASSISTANT.equals(assistant)) {
                // 联网
                request.addCustomParam("enable_internet", StringUtils.defaultString(input.getAssistantToggleValue(), "N"));
                // 文件
                Set<FileUpload> fileUploads = input.getUploadFiles();
                if (CollectionUtils.isNotEmpty(fileUploads)) {
                    request.addCustomParam("file_details", String.join(",", JSON.toJson(fileUploads)));
                }
            }
            if (stackTrace != null) {
                request.addCustomParam("stack_trace", stackTrace);
            }
            // 代码仓库方法体问答
            Editor editor = input.getInput().getEditor();
            if (Objects.nonNull(editor)) {
                SimpleMethodInfo methodInfo = editor.getUserData(MethodAskAIAction.METHOD_INFO);
                if (Objects.nonNull(methodInfo)) {
                    request.addCustomParam("class_name", methodInfo.getClassName());
                    request.addCustomParam("method_name", methodInfo.getMethodName());
                    editor.putUserData(MethodAskAIAction.METHOD_INFO, null);
                }
            }
            // 代码仓库问答
            request.addCustomParam("enable_git_rag", StringUtils.defaultString(input.getGitToggleValue(), "N"));
            if (Constants.DEFAULT_ASSISTANT.equals(assistant) && "Y".equals(input.getGitToggleValue())) {
                request.addCustomParam("revision", GitUtils.getBranchOrRevision(project));
                request.addCustomParam("repo_url", GitUtils.getSSHUrl(project));
            }
            // SQL模式
            request.addCustomParam("enable_sql", StringUtils.defaultString(input.getSqlToggleValue(), "N"));

            String lastParamMd5 = input.getLastParamMd5();
            String paramMd5 = CryptoUtil.getMd5(request.getCustomParam());
            if (!paramMd5.equals(lastParamMd5)) {
                input.setConversationId(null);
                request.setConversationId(null);
            }
            input.setLastParamMd5(paramMd5);
            // 写入请求参数
            OutputStream os = conn.getOutputStream();
            os.write(Objects.requireNonNull(JSON.toJson(request)).getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
            // 读取响应参数

            try (InputStream is = conn.getInputStream();) {
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                while ((line = reader.readLine()) != null) {
                    if (this.isCancelled()) {
                        return null;
                    }
                    if (StringUtils.isBlank(line)) {
                        continue;
                    }
                    if (line.startsWith("data:")) {
                        line = line.substring("data:".length());
                    }
                    Result<ChatCompletionResponse> result = JSON.parse(line, new TypeReference<Result<ChatCompletionResponse>>() {
                    });
                    if (!result.isSuccess()) {
                        ResultHelper.handleResult(result, Constants.Url.CHAT_SEND_MSG_V3);
                        log.warn("call chat api failed, result: {}", line);
                        if (ServiceErrorCode.LLM_CONVERSATION_NOT_EXISTS.getCode().equals(result.getErrorCode())) {
                            input.setConversationId(null);
                            finalMsg = "助手已更新，请重新提问";
                        } else {
                            finalMsg = builder + defaultMsg;
                        }
                        publish(finalMsg);
                        break;
                    }
                    if (questionType == QuestionType.ASK) {
                        Optional.ofNullable(result.getData()).map(ChatCompletionResponse::getConversationId)
                                .ifPresent(x -> questionPanel.getInputPanelFactory().setConversationId(x));
                    }
                    builder.append(result.getData().getAnswer());
                    finalMsg = builder.toString();
                    logId = result.getData().getLogId();
                    messageId = result.getData().getMessageId();
                    publish(finalMsg);
                }
            }
        } catch (Exception e) {
            LOG.debug("call chat api failed: " + e.getMessage());
            if (this.isCancelled()) {
                return null;
            }
            finalMsg = builder + defaultMsg;
            publish(finalMsg);
        }
        return finalMsg;
    }

    @Override
    protected void process(List<String> chunks) {
        if (this.isCancelled()) {
            return;
        }
        String content = chunks.get(chunks.size() - 1);
        if (streamMessageRowFactory == null) {
            renderAnswer(content);
        } else {
            streamMessageRowFactory.flushRender(content);
        }
    }

    public void process(String content) {
        if (streamMessageRowFactory == null) {
            renderAnswer(content);
        } else {
            streamMessageRowFactory.flushRender(content);
        }
    }

    public void renderAnswer(String content, JComponent component) {
        streamMessageRowFactory = new StreamMessageRowFactory(project, viewScrollPane
                , ChatxApplicationSettings.settings().pluginName, content, question, logId, questionType);

        JPanel msgRow = streamMessageRowFactory.create(assistant, messageId);
        if (Objects.nonNull(component)) msgRow.add(component);

//        if (viewPanel.getComponentCount() > 0) {
//            // RoundPanel bug, 暂时无法通过设置border控制间距, 添加empty label来达成效果
//            JLabel empty = new JLabel();
//            empty.setPreferredSize(new Dimension(-1, 2));
//            viewPanel.add(empty);
//        }

        viewPanel.add(msgRow);
        if (StringUtils.isNotEmpty(question)) {
            viewPanel.add(createReAnswer());
        }
        viewPanel.repaint(msgRow.getBounds());
        streamMessageRowFactory.render();
    }

    public void renderAnswer(String content) {
        renderAnswer(content, null);
    }

    private Component createReAnswer() {
        reAnswerOrStop = new JLabel();
        reAnswerOrStop.setForeground(JBColor.BLUE);
        reAnswerOrStop.setIcon(Icons.LOADING_ANIMATED);
        reAnswerOrStop.setText("停止生成");
        reAnswerOrStop.setBorder(BorderFactory.createEmptyBorder(-2, 10, 10, 0));
        reAnswerOrStop.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (reAnswerOrStop.getText().contains("停止")) {
                    reAnswerOrStop.setText("停止生成");
                } else {
                    reAnswerOrStop.setText("重新生成");
                }
                super.mouseExited(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (reAnswerOrStop.getText().contains("停止")) {
                    String uTxt = String.format("<html><u style=\"color: rgb(88,157,246);\">%s</u></html>", "停止生成");
                    reAnswerOrStop.setText(uTxt);
                } else {
                    String uTxt = String.format("<html><u style=\"color: rgb(88,157,246);\">%s</u></html>", "重新生成");
                    reAnswerOrStop.setText(uTxt);
                }
                super.mouseEntered(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                streamMessageRowFactory.getReAnswer().setVisible(true);
                reAnswerOrStop.setVisible(false);
                CancelableButton sendBtn = questionPanel.getInputPanelFactory().getSendBtn();
                if (sendBtn.isCancelable()) {
                    sendBtn.doClick();
                }
            }
        });

        return reAnswerOrStop;
    }

    @Override
    public void done() {
        // add suggest question
        if (StringUtils.isNotEmpty(messageId) && QuestionType.ASK == questionType) {
            AssistantInfo assistantInfo = ChatxApplicationSettings.settings().getAssistantInfo(assistant);
            AssistantDetail detail = assistantInfo.getDetail();
            boolean enabled = Objects.nonNull(detail) ? detail.getSuggestedQuestionsAfterAnswer().getEnabled() : false;
            if (enabled && Objects.nonNull(streamMessageRowFactory.getMessageBodyFactory())) {
                streamMessageRowFactory.getMessageBodyFactory().addSuggestQuestion(messageId, this::resetUI);
            } else {
                resetUI();
            }
        } else {
            resetUI();
        }
    }

    private void resetUI() {
        if (Objects.nonNull(streamMessageRowFactory)) streamMessageRowFactory.done();
        if (Objects.nonNull(questionPanel)) questionPanel.askAiDone();
        if (Objects.nonNull(reAnswerOrStop)) {
//            reAnswerOrStop.setIcon(null);
//            reAnswerOrStop.setText("重新生成");
            reAnswerOrStop.setVisible(false);
        }
    }

    private HttpURLConnection getHttpURLConnection() throws IOException {
        String urlStr = Constants.Url.CHAT_SEND_MSG_V3;
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setReadTimeout(READ_TIME_OUT);
        // set header
        conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
        conn.setRequestProperty("Accept", "text/event-stream");
        // base header
        RestTemplateUtil.baseHeaders().forEach(conn::setRequestProperty);
        // add common datasets header
        boolean withDataset = ChatxApplicationSettings.settings().withDataset;
        conn.setRequestProperty(Constants.Headers.WITH_DATA_SET, withDataset ? "Y" : "N");
        // add defined datasets header
        extractDataset(conn, assistant);

        return conn;
    }

    private void extractDataset(HttpURLConnection conn, String assistant) {
        if (question.contains("#")) {
            Set<String> datasetIds = new HashSet<>();
            List<DatasetInfo> datasetList = ChatxApplicationSettings.settings().getDatasets(assistant).stream()
                    .toList();
            if (CollectionUtils.isEmpty(datasetList)) return;

            // #dataset
            for (DatasetInfo dataset : datasetList) {
                String datasetName = "#" + dataset.getDatasetName();
                if (question.contains(datasetName)) {
                    datasetIds.add(dataset.getDatasetId());
                    // 不做剪切, 交由服务端处理
//                    question = StringUtils.substringBefore(question, datasetName) +
//                            StringUtils.substringAfter(question, datasetName);
                }
            }
            // #all
            if (question.contains(Constants.ALL_DATASET)) {
                question = question.replace(Constants.ALL_DATASET, "");
                for (DatasetInfo datasetInfo : datasetList) {
                    datasetIds.add(datasetInfo.getDatasetId());
                }
            }

            question = question.trim();
            conn.setRequestProperty(Constants.Headers.WITH_DATA_SET_ID, String.join(",", datasetIds));
        }
    }
}
