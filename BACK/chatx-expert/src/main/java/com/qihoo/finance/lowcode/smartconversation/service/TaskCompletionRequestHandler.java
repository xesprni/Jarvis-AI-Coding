package com.qihoo.finance.lowcode.smartconversation.service;


import com.intellij.openapi.project.Project;
import com.qifu.ui.smartconversation.settings.service.TaskCompletionParameters;
import com.qifu.ui.smartconversation.sse.AgentJob;
import com.qifu.ui.smartconversation.sse.CompletionProgressNotifier;
import com.qifu.ui.smartconversation.sse.EventSourceToAgentAdapter;
import com.qihoo.finance.lowcode.smartconversation.utils.ErrorDetails;

/**
 * @author weiyichao
 * @date 2025-09-30
 **/
public class TaskCompletionRequestHandler {
    private final Project project;
    private final CompletionResponseEventListener completionResponseEventListener;
    private AgentJob agentJob;
    private TaskCompletionEventListener taskCompletionEventListener;


    public TaskCompletionRequestHandler(
            Project project,
            CompletionResponseEventListener completionResponseEventListener) {
        this.project = project;
        this.completionResponseEventListener = completionResponseEventListener;
    }

    public void call(TaskCompletionParameters callParameters) {
        try {
            agentJob = startCall(callParameters);
        } catch (TotalUsageExceededException e) {
            System.out.printf(e.getMessage());
        }
    }

    public void cancel(String taskId) {
        if (agentJob != null) {
            agentJob.cancel(taskId);
            if (taskCompletionEventListener != null) {
                taskCompletionEventListener.onCancelled(new StringBuilder());
            }
        }
    }

    private AgentJob startCall(TaskCompletionParameters callParameters) {
        try {
            CompletionProgressNotifier.Companion.update(project, true);
            taskCompletionEventListener = new TaskCompletionEventListener(
                    project,
                    callParameters,
                    completionResponseEventListener
            );
            return EventSourceToAgentAdapter.INSTANCE.getChatCompletionAsync(
                    callParameters,
                    taskCompletionEventListener,
                    project

            );
        } catch (Throwable ex) {
            handleCallException(ex);
            throw ex;
        }
    }

    private void handleCallException(Throwable ex) {
        var errorMessage = "Something went wrong";
        if (ex instanceof TotalUsageExceededException) {
            errorMessage =
                    "The length of the context exceeds the maximum limit that the model can handle. "
                            + "Try reducing the input message or maximum completion token size.";
        }
        completionResponseEventListener.handleError(new ErrorDetails(errorMessage), ex);
    }
}
