package com.qihoo.finance.lowcode.common.util;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.BuildNumber;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ApplicationUtil {

    @Nullable
    public static Project findCurrentProject() {
        IdeFrame frame = IdeFocusManager.getGlobalInstance().getLastFocusedFrame();
        Project project = (frame != null) ? frame.getProject() : null;
        if (isValidProject(project))
            return project;
        for (Project p : ProjectManager.getInstance().getOpenProjects()) {
            if (isValidProject(p))
                return p;
        }
        return null;
    }

    @Nonnull
    public static Iterable<Project> findValidProjects() {
        return (Iterable<Project>) Arrays.<Project>stream(ProjectManager.getInstance().getOpenProjects())
                .filter(ApplicationUtil::isValidProject)
                .collect(Collectors.toList());
    }

    private static boolean isValidProject(@Nullable Project project) {
        return (project != null && !project.isDisposed() && !project.isDefault());
    }

    public static boolean isBaselineVersionEgt(int baselineVersion) {
        BuildNumber build = ApplicationInfo.getInstance().getBuild();
        return build.getBaselineVersion() >= baselineVersion;
    }

    public static int compareSemVer(String v1, String v2) {
        String[] a1 = v1.split("[-+]");
        String[] a2 = v2.split("[-+]");
        String[] nums1 = a1[0].split("\\.");
        String[] nums2 = a2[0].split("\\.");

        for (int i = 0; i < Math.max(nums1.length, nums2.length); i++) {
            int n1 = i < nums1.length ? Integer.parseInt(nums1[i]) : 0;
            int n2 = i < nums2.length ? Integer.parseInt(nums2[i]) : 0;
            if (n1 != n2) return n1 - n2;
        }

        // 主体版本相同，处理预发布标签（如 rc1、alpha）
        if (a1.length == 1 && a2.length > 1) return 1;
        if (a1.length > 1 && a2.length == 1) return -1;
        if (a1.length == 1) return 0;
        return a1[1].compareTo(a2[1]);
    }

}
