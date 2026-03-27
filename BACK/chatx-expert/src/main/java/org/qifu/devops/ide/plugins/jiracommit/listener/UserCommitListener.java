package org.qifu.devops.ide.plugins.jiracommit.listener;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface UserCommitListener {

    Topic<UserCommitListener> TOPIC = Topic.create("vcs.commit", UserCommitListener.class);

    void onCommit(Project currProject, @NotNull String commitMsg);
}
