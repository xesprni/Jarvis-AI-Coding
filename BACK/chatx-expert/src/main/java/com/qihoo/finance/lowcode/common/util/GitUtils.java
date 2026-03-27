package com.qihoo.finance.lowcode.common.util;


import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.qihoo.finance.lowcode.gentracker.tool.ProjectUtils;
import com.qihoo.finance.lowcode.settings.ChatxApplicationSettings;
import git4idea.index.GitFileStatus;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GitUtils
 *
 * @author fengjinfu-jk
 * date 2023/10/30
 * @version 1.0.0
 * @apiNote GitUtils
 */
@Slf4j
public class GitUtils {
    public static String getCurrentProjectGitUrl() {
        return getGitUrl(ProjectUtils.getCurrProject());
    }

    public static String getCurrentProjectBranchName() {
        return getBranchName(ProjectUtils.getCurrProject());
    }

    public static String getSSHUrl(Project project, String httpsHost, String sshHost) {
        if (project.isDisposed()) return StringUtils.EMPTY;

        String projectGitUrl = getGitUrl(project);
        if (StringUtils.isBlank(projectGitUrl)) return projectGitUrl;
        if (projectGitUrl.startsWith("ssh")) return projectGitUrl;

        return projectGitUrl.replace(httpsHost, sshHost);
    }

    public static String getSSHUrl(Project project) {
        if (project.isDisposed()) return StringUtils.EMPTY;

        return getSSHUrl(project, ChatxApplicationSettings.settings().gitlabHttpsHost,
                ChatxApplicationSettings.settings().gitlabSshHost);
    }

    public static String getGitUrl(Project project) {
        if (project.isDisposed()) return StringUtils.EMPTY;

        GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
        for (GitRepository repository : repositoryManager.getRepositories()) {
            Collection<GitRemote> remotes = repository.getRemotes();

            if (CollectionUtils.isNotEmpty(remotes)) {
                return remotes.stream().filter(remote -> remote.getName().equals(GitRemote.ORIGIN))
                        .findFirst().orElse(remotes.iterator().next()).getFirstUrl();
            }
        }

        log.error("GitRepositoryManager getProjectGitUrl fail");
        return StringUtils.EMPTY;
    }

    public static Set<String> getGitUrls() {
        Project project = ProjectUtils.getCurrProject();
        if (project.isDisposed()) return new HashSet<>();

        GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
        return ListUtils.defaultIfNull(repositoryManager.getRepositories(), new ArrayList<>())
                .stream().flatMap(repository -> repository.getRemotes().stream())
                .flatMap(remote -> remote.getUrls().stream()).collect(Collectors.toSet());
    }

    public static String getBranchName(Project project) {
        if (project.isDisposed()) return StringUtils.EMPTY;

        GitRepositoryManager repositoryManager = GitRepositoryManager.getInstance(project);
        for (GitRepository repository : repositoryManager.getRepositories()) {
            return repository.getCurrentBranchName();
        }

        return StringUtils.EMPTY;
    }

    public static String getCurrentSimpleRevision(Project project) {
        if (project.isDisposed()) return StringUtils.EMPTY;

        String currentRevision = getRevision(project);
        if (StringUtils.isNotBlank(currentRevision) && currentRevision.length() > 7) {
            return currentRevision.substring(0, 8);
        }

        return currentRevision;
    }

    public static String getRevision(Project project) {
        if (project.isDisposed()) return StringUtils.EMPTY;

        List<GitRepository> repositories = GitRepositoryManager.getInstance(project).getRepositories();
        for (GitRepository repository : repositories) {
            return repository.getInfo().getCurrentRevision();
        }

        return StringUtils.EMPTY;
    }

    public static String getBranchOrRevision(Project project) {
        return StringUtils.defaultIfEmpty(getBranchName(project), getRevision(project));
    }

    public static List<VirtualFile> getStagingFiles(Project project) {
        if (project.isDisposed()) return new ArrayList<>();

        List<GitRepository> repositories = GitRepositoryManager.getInstance(project).getRepositories();
        for (GitRepository repository : repositories) {
            List<GitFileStatus> allRecords = repository.getStagingAreaHolder().getAllRecords();
            return allRecords.stream().map(GitFileStatus::getPath)
                    .map(FilePath::getVirtualFile)
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    public static List<Change> getAllChanges(Project project) {
        if (project.isDisposed()) return new ArrayList<>();

        // Change.getBeforeRevision().getContent() 获取变更前内容
        // Change.getAfterRevision().getContent() 获取变更后内容
        return ((List<Change>) ChangeListManager.getInstance(project).getAllChanges());
    }
}
