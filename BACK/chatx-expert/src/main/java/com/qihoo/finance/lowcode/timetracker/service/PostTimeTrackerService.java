package com.qihoo.finance.lowcode.timetracker.service;

import com.intellij.AppTopics;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import com.qihoo.finance.lowcode.common.configuration.UserInfoPersistentState;
import com.qihoo.finance.lowcode.common.entity.timetrack.ReadWritePushDTO;
import com.qihoo.finance.lowcode.common.entity.timetrack.ReadWritePushResponse;
import com.qihoo.finance.lowcode.common.util.GitUtils;
import com.qihoo.finance.lowcode.common.util.LowCodeAppUtils;
import com.qihoo.finance.lowcode.common.util.RestTemplateUtil;
import com.qihoo.finance.lowcode.console.mongo.view.editor.MongoEditor;
import com.qihoo.finance.lowcode.console.mysql.editor.SQLDataEditor;
import com.qihoo.finance.lowcode.gentracker.tool.PluginUtils;
import com.qihoo.finance.lowcode.timetracker.configuration.DefaultSettingsService;
import com.qihoo.finance.lowcode.timetracker.configuration.TimeTrackerPersistentState;
import com.qihoo.finance.lowcode.timetracker.configuration.TimeTrackingStatus;
import com.qihoo.finance.lowcode.timetracker.factory.TimeTrackerWidget;
import com.qihoo.finance.lowcode.timetracker.utils.TimeTrackerUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.*;

import static com.qihoo.finance.lowcode.timetracker.utils.TimeTrackerUtil.msToS;

/**
 * @author weiyichao
 * @date 2023-07-21
 **/
@Slf4j
@State(name = "LowCodeTimeTracker", storages = {@Storage(value = StoragePathMacros.WORKSPACE_FILE)})
public final class PostTimeTrackerService implements PersistentStateComponent<TimeTrackerPersistentState>, Disposable {
    @Nullable
    private TimeTrackerWidget widget;
    @NotNull
    public final Project project;
    /**
     * 写数据处理
     */
    @Nullable
    private ScheduledFuture<?> writeTicker;
    /**
     * 读数据处理
     */
    @Nullable
    private ScheduledFuture<?> readTicker;
    @Nullable
    private ScheduledFuture<?> updateConfigHandler;
    @Nullable
    private ScheduledFuture<?> calculator;
    private static final boolean DEBUG_LIFECYCLE = false;
    public static final long RESET_TIME_TO_ZERO = Long.MIN_VALUE;
    private volatile TimeTrackingStatus writeStatus = TimeTrackingStatus.STOPPED;
    private volatile TimeTrackingStatus readStatus = TimeTrackingStatus.STOPPED;
    private static final TimeUnit TICK_DELAY_UNIT = TimeUnit.SECONDS;
    private static final long TICK_DELAY = 1;
    private static final long TICK_JUMP_DETECTION_THRESHOLD_MS = TICK_DELAY_UNIT.toMillis(TICK_DELAY * 20);
    private static final Set<PostTimeTrackerService> ALL_OPENED_TRACKERS = ContainerUtil.newConcurrentSet();
    public static final String EVENT_FROM_WRITE = "Write";
    public static final String EVENT_FROM_READ = "Read";
    /**
     * 配置更新间隔
     */
    private static final long UPDATE_CONFIG_DELAY_SECONDS = 60 * 60;
    /**
     * 统计间隔
     */
    private static final long CALCULATE_DELAY_SECONDS = 60;
    private static final long CALCULATE_DELAY_MS = CALCULATE_DELAY_SECONDS * 1000;
    /**
     * 写耗时
     */
    private long writeTotalTimeMs = 0;
    /**
     * 读耗时
     */
    private long readTotalTimeMs = 0;
    private long writeTimeSecondsAfterLastCommit = 0;
    private long readTimeSecondsAfterLastCommit = 0;
    private long writeStatusStartedMs = System.currentTimeMillis();
    private long readStatusStartedMs = System.currentTimeMillis();
    private long writeLastTickMs = System.currentTimeMillis();
    private long readLastTickMs = System.currentTimeMillis();
    private volatile long writeLastActivityMs = System.currentTimeMillis();
    private volatile long readLastActivityMs = System.currentTimeMillis();
    private boolean autoStart;
    private long idleThresholdMs;
    private int writeAutoCountIdleSeconds;
    private int readAutoCountIdleSeconds;
    private boolean stopWhenIdleRatherThanPausing;
    private boolean pauseOtherTrackerInstances;
    private String reportDate;
    //------------------------------------------------------------------------------------------------------------------
    private long preTimeMillis = 0L;
    private long preWriteTotalTime = 0L;
    private long preReadTotalTime = 0L;
    /**
     * 首次启动时间
     */
    private Date lastReportDate;
    private int maxPushBufferSize = 360;
    private int pushBufferSize = 1;
    private final ArrayDeque<ReadWritePushDTO> pushBuffer = new ArrayDeque<>();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 20, 120L,
            TimeUnit.MINUTES
            , new LinkedBlockingQueue<>(10));

    //------------------------------------------------------------------------------------------------------------------

    public PostTimeTrackerService(@NotNull Project project) {
//        if (DEBUG_LIFECYCLE) {
        log.debug("[TimeTrack] Instantiated {}", this);
//        }
        this.project = project;
        Disposer.register(project, this);
        ALL_OPENED_TRACKERS.add(this);
        startScheduler();
        project.getMessageBus()
                .connect(this)
                .subscribe(
                        AppTopics.FILE_DOCUMENT_SYNC,
                        new FileDocumentManagerListener() {
                            @Override
                            public void beforeAllDocumentsSaving() {
                                saveTime();
                            }

                            @Override
                            public void beforeDocumentSaving(@NotNull Document document) {
                                saveTime();
                            }

                            // Default methods in 2018.3, but would probably crash in earlier versions
                            @Override
                            public void beforeFileContentReload(
                                    @NotNull VirtualFile file, @NotNull Document document) {
                            }
                        });
        EditorFactory.getInstance()
                .getEventMulticaster()
                .addDocumentListener(
                        new DocumentListener() {
                            @Override
                            public void documentChanged(@NotNull DocumentEvent e) {
                                if (!isAutoStart() || getWriteStatus() == TimeTrackingStatus.RUNNING) {
                                    return;
                                }
                                EventQueue.invokeLater(
                                        () -> {
                                            final Project project = project();
                                            if (project == null) {
                                                return;
                                            }

                                            FileEditorManager editorManager = FileEditorManager.getInstance(project);
                                            final Editor selectedTextEditor = editorManager.getSelectedTextEditor();
                                            final FileEditor editor = editorManager.getSelectedEditor();

                                            if (selectedTextEditor != null && !selectedTextEditor.isViewer()) {
                                                selectedTextEditor.getDocument();
                                                if (e.getDocument().equals(selectedTextEditor.getDocument())) {
                                                    setStatus(TimeTrackingStatus.RUNNING, EVENT_FROM_WRITE);
                                                }
                                            }
                                            if (editor instanceof MongoEditor || editor instanceof SQLDataEditor) {
                                                setStatus(TimeTrackingStatus.RUNNING, EVENT_FROM_WRITE);
                                            }
                                        });
                            }
                        },
                        this);

        InactivityService.getInstance().assignProjectWindow(this, null);
    }


    @Nullable
    private Project project() {
        final Project project = this.project;
        if (project.isDisposed()) {
            return null;
        }
        return project;
    }

    @NotNull
    public TimeTrackerWidget widget() {
        TimeTrackerWidget widget;
        synchronized (this) {
            widget = this.widget;
            if (widget == null) {
                this.widget = widget = new TimeTrackerWidget(this);
            }
        }
        return widget;
    }

    @NotNull
    public TimeTrackingStatus getWriteStatus() {
        return writeStatus;
    }

    public synchronized void toggleRunning(String from) {
        if (EVENT_FROM_READ.equals(from)) {
            toggleRunning(this.readStatus, EVENT_FROM_READ);
        } else {
            toggleRunning(this.writeStatus, EVENT_FROM_WRITE);
        }
    }

    private void toggleRunning(TimeTrackingStatus status, String from) {
        switch (status) {
            case RUNNING:
                setStatus(TimeTrackingStatus.STOPPED, from);
                break;
            case STOPPED:
            case IDLE:
                setStatus(TimeTrackingStatus.RUNNING, from);
                break;
            default:
                break;
        }
    }


    private synchronized void readTick() {
        if (this.readStatus != TimeTrackingStatus.RUNNING) {
            log.warn("[TimeTrack] Tick when status is : {}", this.readStatus);
            return;
        }
        final long now = System.currentTimeMillis();
        tick(now, this.readLastTickMs, this.readLastActivityMs, EVENT_FROM_READ);
        readLastTickMs = now;
        repaintWidget(false);
    }

    private synchronized void writeTick() {
        if (this.writeStatus != TimeTrackingStatus.RUNNING) {
            log.warn("[TimeTrack] Tick when status is {}", writeStatus);
            return;
        }
        final long now = System.currentTimeMillis();
        tick(now, this.writeLastTickMs, this.writeLastActivityMs, EVENT_FROM_WRITE);
        writeLastTickMs = now;
        repaintWidget(false);
    }

    private void tick(final long now, long lastTickMs, long lastActivityMs, String from) {
        final long sinceLastTickMs = now - lastTickMs;
        final long sinceLastActivityMs = now - lastActivityMs;

        // 是否追加projectFocus判定
        if (sinceLastTickMs > TICK_JUMP_DETECTION_THRESHOLD_MS) {
            final long lastValidTimeMs = readLastTickMs + TICK_JUMP_DETECTION_THRESHOLD_MS;
            setStatus(
                    stopWhenIdleRatherThanPausing ? TimeTrackingStatus.STOPPED : TimeTrackingStatus.IDLE,
                    lastValidTimeMs, from);
        } else if (sinceLastActivityMs >= idleThresholdMs) {
            final long lastValidTimeMs = lastActivityMs + idleThresholdMs;
            setStatus(
                    stopWhenIdleRatherThanPausing ? TimeTrackingStatus.STOPPED : TimeTrackingStatus.IDLE,
                    lastValidTimeMs, from);
        }
    }

    private synchronized void otherComponentStarted(String from) {
        if (EVENT_FROM_READ.equals(from)) {
            if (this.readStatus != TimeTrackingStatus.STOPPED) {
                setStatus(TimeTrackingStatus.IDLE, EVENT_FROM_READ);
            }
        } else {
            if (this.writeStatus != TimeTrackingStatus.STOPPED) {
                setStatus(TimeTrackingStatus.IDLE, EVENT_FROM_WRITE);
            }
        }
    }

    private synchronized void addOrResetTotalTimeMs(long milliseconds) {
        if (milliseconds == RESET_TIME_TO_ZERO) {
            writeTotalTimeMs = 0L;
            readTotalTimeMs = 0L;
            readStatusStartedMs = System.currentTimeMillis();
            writeStatusStartedMs = System.currentTimeMillis();
        } else {
            addTotalTimeMs(milliseconds, EVENT_FROM_READ);
            addTotalTimeMs(milliseconds, EVENT_FROM_WRITE);
        }
        repaintWidget(false);
    }

    private synchronized void addTotalTimeMs(long milliseconds, String from) {
        if (EVENT_FROM_READ.equals(from)) {
            readTotalTimeMs = Math.max(0L, readTotalTimeMs + milliseconds);
        } else {
            writeTotalTimeMs = Math.max(0L, writeTotalTimeMs + milliseconds);
        }
    }

    private synchronized void saveTime() {
        if (this.writeStatus == TimeTrackingStatus.RUNNING) {
            long now = System.currentTimeMillis();
            long msInState = Math.max(0L, now - writeStatusStartedMs);
            writeStatusStartedMs = now;
            addTotalTimeMs(msInState, EVENT_FROM_WRITE);
        }
    }

    private synchronized void setStatus(TimeTrackingStatus status, String from) {
        setStatus(status, System.currentTimeMillis(), from);
    }

    private void setStatus(TimeTrackingStatus status, long now, String from) {
        if (EVENT_FROM_READ.equals(from)) {
            setReadStatus(status, now);
        } else {
            setWriteStatus(status, now);
        }
    }

    private void setReadStatus(TimeTrackingStatus status, long now) {
        if (this.readStatus == status) {
            return;
        }
        if (readTicker != null) {
            readTicker.cancel(false);
            readTicker = null;
        }

        long msInState = Math.max(0L, now - readStatusStartedMs);

        switch (this.readStatus) {
            case RUNNING: {
                addTotalTimeMs(msInState, EVENT_FROM_READ);
                break;
            }
            case IDLE: {
                if (msToS(msInState) <= writeAutoCountIdleSeconds) {
                    addTotalTimeMs(msInState, EVENT_FROM_READ);
                }
                break;
            }
            default:
                break;
        }
        readStatusStartedMs = now;
        readLastTickMs = now;
        readLastActivityMs = now;
        this.readStatus = status;
        if (status == TimeTrackingStatus.RUNNING) {
            if (pauseOtherTrackerInstances) {
                ALL_OPENED_TRACKERS.forEach(
                        tracker -> {
                            if (tracker != this) {
                                tracker.otherComponentStarted(EVENT_FROM_READ);
                            }
                        });

            }
            readTicker = EdtExecutorService.getScheduledExecutorInstance()
                    .scheduleWithFixedDelay(this::readTick, TICK_DELAY, TICK_DELAY, TICK_DELAY_UNIT);
        }
        repaintWidget(false);
    }

    private void setWriteStatus(TimeTrackingStatus status, long now) {
        if (this.writeStatus == status) {
            return;
        }
        if (writeTicker != null) {
            writeTicker.cancel(false);
            writeTicker = null;
        }
        long msInState = Math.max(0L, now - writeStatusStartedMs);

        switch (this.writeStatus) {
            case RUNNING: {
                addTotalTimeMs(msInState, EVENT_FROM_WRITE);
                break;
            }
            case IDLE: {
                if (msToS(msInState) <= writeAutoCountIdleSeconds) {
                    addTotalTimeMs(msInState, EVENT_FROM_WRITE);
                }
                break;
            }
            default:
                break;
        }
        writeStatusStartedMs = now;
        writeLastTickMs = now;
        writeLastActivityMs = now;
        this.writeStatus = status;
        if (status == TimeTrackingStatus.RUNNING) {
            if (pauseOtherTrackerInstances) {
                ALL_OPENED_TRACKERS.forEach(
                        tracker -> {
                            if (tracker != this) {
                                tracker.otherComponentStarted(EVENT_FROM_WRITE);
                            }
                        });

            }
            writeTicker = EdtExecutorService.getScheduledExecutorInstance()
                    .scheduleWithFixedDelay(this::writeTick, TICK_DELAY, TICK_DELAY, TICK_DELAY_UNIT);

        }
        repaintWidget(false);
    }

    public synchronized long getTotalTimeMs(String from) {
        long resultMs;
        if (EVENT_FROM_READ.equals(from)) {
            resultMs = readTotalTimeMs;
            if (readStatus == TimeTrackingStatus.RUNNING) {
                long now = System.currentTimeMillis();
                resultMs += Math.max(0L, now - readStatusStartedMs);
            }
        } else {
            resultMs = writeTotalTimeMs;
            if (writeStatus == TimeTrackingStatus.RUNNING) {
                long now = System.currentTimeMillis();
                resultMs += Math.max(0L, now - writeStatusStartedMs);
            }
        }
        return resultMs;
    }

    public long getWriteTimeSecondsAfterLastCommit() {
        return writeTimeSecondsAfterLastCommit;
    }

    public synchronized void setWriteTimeSecondsAfterLastCommit(long writeTimeSecondsAfterLastCommit) {
        this.writeTimeSecondsAfterLastCommit = writeTimeSecondsAfterLastCommit;
    }

    public long getReadTimeSecondsAfterLastCommit() {
        return readTimeSecondsAfterLastCommit;
    }

    public synchronized void setReadTimeSecondsAfterLastCommit(long readTimeSecondsAfterLastCommit) {
        this.readTimeSecondsAfterLastCommit = readTimeSecondsAfterLastCommit;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public synchronized void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public long getIdleThresholdMs() {
        return idleThresholdMs;
    }

    public synchronized void setIdleThresholdMs(long idleThresholdMs) {
        this.idleThresholdMs = idleThresholdMs;
    }

    public synchronized void setWriteAutoCountIdleSeconds(int writeAutoCountIdleSeconds) {
        this.writeAutoCountIdleSeconds = writeAutoCountIdleSeconds;
    }

    public synchronized void setReadAutoCountIdleSeconds(int readAutoCountIdleSeconds) {
        this.readAutoCountIdleSeconds = readAutoCountIdleSeconds;
    }

    public boolean isStopWhenIdleRatherThanPausing() {
        return stopWhenIdleRatherThanPausing;
    }

    public void setStopWhenIdleRatherThanPausing(boolean stopWhenIdleRatherThanPausing) {
        this.stopWhenIdleRatherThanPausing = stopWhenIdleRatherThanPausing;
    }

    public boolean isPauseOtherTrackerInstances() {
        return pauseOtherTrackerInstances;
    }

    public synchronized void setPauseOtherTrackerInstances(boolean pauseOtherTrackerInstances) {
        this.pauseOtherTrackerInstances = pauseOtherTrackerInstances;
    }

    public synchronized void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    @Override
    public String toString() {
        return "TTC(" + project + ")@" + System.identityHashCode(this);
    }

    private synchronized void updateConfig() {
        // log.info("[TimeTrack] LowCodeTimeTrackerService#updateConfig");
        new SwingWorker<TimeTrackerPersistentState, TimeTrackerPersistentState>() {
            @Override
            protected TimeTrackerPersistentState doInBackground() throws Exception {
                return RestTemplateUtil.getConfig();
            }

            @Override
            protected void done() {
                try {
                    TimeTrackerPersistentState timeTrackerPersistentState = get();
                    if (timeTrackerPersistentState != null) {
                        autoStart = timeTrackerPersistentState.autoStart;
                        idleThresholdMs = timeTrackerPersistentState.idleThresholdMs;
                        writeAutoCountIdleSeconds = timeTrackerPersistentState.writeAutoCountIdleSeconds;
                        readAutoCountIdleSeconds = timeTrackerPersistentState.readAutoCountIdleSeconds;
                        stopWhenIdleRatherThanPausing = timeTrackerPersistentState.stopWhenIdleRatherThanPausing;
                        pauseOtherTrackerInstances = timeTrackerPersistentState.pauseOtherTrackerInstances;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    // ignore
                    log.info("[TimeTrack] updateConfig fail: {}", e.getMessage(), e);
                }

                super.done();
            }
        }.execute();
    }

    private boolean lostFocus = true;
    private boolean focusIde = true;

    private synchronized void calculate() {
        if (!LowCodeAppUtils.isLogin()) return;
        if (!LowCodeAppUtils.isLastFocusProject(project)) return;
        if (LowCodeAppUtils.isFocusIde()) {
            focusIde = true;
        } else {
            // 连续两次检查都未曾获取到焦点，则认为失去焦点
            if (!focusIde) {
                // log.info("[TimeTrack] [{}] 未检测到焦点2, 连续两次检查都未曾获取到焦点, 停止计时", project.getName());
                return;
            }
            // log.info("[TimeTrack] [{}] 未检测到焦点1, 预备停止计时", project.getName());
            focusIde = false;
        }

        long currentTimeMillis = System.currentTimeMillis();
        long readTotalMs = getTotalTimeMs(EVENT_FROM_READ);
        long writeTotalMs = getTotalTimeMs(EVENT_FROM_WRITE);

        if (Math.abs(msToS(preReadTotalTime) - msToS(readTotalMs)) <= 2 && Math.abs(msToS(preWriteTotalTime) - msToS(writeTotalMs)) <= 2) {
            if (!lostFocus) {
                lostFocus = true;
                // log.info("[TimeTrack] [{}] 未获取读写事件响应, 窗口失焦, 停止计时", project.getName());
            }
            return;
        }

        lostFocus = false;

        try {
            if (lastReportDate == null) {
                // IDE启动, 数据无需上报, 直接到finally更新本地读写持久化累计值
                lastReportDate = new Date();
                return;
            }
            long readMs = Math.min(Math.abs(readTotalMs - preReadTotalTime), CALCULATE_DELAY_MS);
            long writeMs = Math.min(Math.abs(writeTotalMs - preWriteTotalTime), CALCULATE_DELAY_MS);
            // log.info("[TimeTrack] [{}] 获取读写事件响应, 窗口获得焦点, 触发计时， readMs: {}, writeMs: {}", project.getName(), readMs, writeMs);
            // 异步组装数据并推送
            postTimeTrack(readMs, writeMs);
        } finally {
            preTimeMillis = currentTimeMillis;
            preWriteTotalTime = writeTotalMs;
            preReadTotalTime = readTotalMs;
        }

    }

    private void postTimeTrack(long readTime, long writeTime) {
        // 仓库、分支、开发人、需求id、耗时、上报时间
        ReadWritePushDTO rwPush = new ReadWritePushDTO();
        // developer
        rwPush.setUserNo(getUserNo());
        // project
        rwPush.setProjectName(project.getName());
        // Git url
        String gitUrl = GitUtils.getGitUrl(project);
        rwPush.setGitUrl(gitUrl);
        // Git branch
        String gitBranch = GitUtils.getBranchName(project);
        rwPush.setGitBranch(gitBranch);
        rwPush.setCycleTime(CALCULATE_DELAY_MS);
        rwPush.setReadTime(readTime);
        rwPush.setWriteTime(writeTime);
        // ide version
        rwPush.setIdeVersion(ApplicationInfo.getInstance().getFullVersion());
        // plugin version
        rwPush.setPluginVersion(PluginUtils.getPluginVersion());
        // date
        rwPush.setReportDate(new Date());
        rwPush.setLastReportDate(lastReportDate);

        // 添加到缓冲区, 先进先出, 最大缓冲 maxPostBufferSize, 简单方式短时间网络问题
        if (pushBuffer.size() >= maxPushBufferSize) {
            pushBuffer.poll();
        }
        pushBuffer.add(rwPush);
        if (pushBuffer.size() >= pushBufferSize) {
            // 异步推送, 推送成功后, 才清零 postBuffer
            asyncBatchPushBuffer();
        }
    }

    public void asyncBatchPushBuffer() {
        executor.execute(() -> {
            ReadWritePushResponse response = TimeTrackerUtil.batchPushTimeTrack(pushBuffer);
            if (response == null) {
                log.warn("[TimeTrack] TimeTrackerUtil.batchPushTimeTrack fail, response data is null");
                return;
            }
            if (response.isSuccess()) {
                pushBuffer.clear();
                lastReportDate = new Date();
            }
            this.maxPushBufferSize = response.getMaxPushBufferSize();
            this.pushBufferSize = response.getPushBufferSize();
        });
    }

    private String getUserNo() {
        UserInfoPersistentState userInfoPersistentState = UserInfoPersistentState.getInstance();
        UserInfoPersistentState.UserInfo user = userInfoPersistentState.getState();
        if (user == null) {
            log.warn("[TimeTrack] 用户未登录，无法获取用户信息");
            return StringUtils.EMPTY;
        }
        return user.getUserNo();
    }

    public void resetTimeSecondsAfterCommit() {
        this.writeTimeSecondsAfterLastCommit = 0L;
        this.readTimeSecondsAfterLastCommit = 0L;
        loadState(getState());
    }

    private void addTimeSecondsAfterCommit(long writeTimeToShow, long readTimeToShow) {
        this.writeTimeSecondsAfterLastCommit += Math.min(Math.max(0L, writeTimeToShow), CALCULATE_DELAY_SECONDS);
        this.readTimeSecondsAfterLastCommit += Math.min(Math.max(0L, readTimeToShow), CALCULATE_DELAY_SECONDS);
    }


    private void startScheduler() {
        if (updateConfigHandler != null) {
            updateConfigHandler.cancel(false);
            updateConfigHandler = null;
        }

        updateConfigHandler = EdtExecutorService.getScheduledExecutorInstance()
                .scheduleWithFixedDelay(this::updateConfig, TICK_DELAY, UPDATE_CONFIG_DELAY_SECONDS, TICK_DELAY_UNIT);


        if (calculator != null) {
            calculator.cancel(false);
            calculator = null;
        }

        calculator = EdtExecutorService.getScheduledExecutorInstance()
                .scheduleWithFixedDelay(this::calculate, TICK_DELAY, CALCULATE_DELAY_SECONDS, TICK_DELAY_UNIT);
    }


    private void repaintWidget(boolean relayout) {
        final TimeTrackerWidget widget = this.widget;
        if (widget != null) {
            UIUtil.invokeLaterIfNeeded(
                    () -> {
                        widget.repaint();
                        if (relayout) {
                            widget.revalidate();
                        }
                    });
        }
    }

    public void notifyUserNotIdle() {
        long now = System.currentTimeMillis();
        readLastActivityMs = now;
        if (readStatus == TimeTrackingStatus.IDLE) {
            synchronized (this) {
                if (readStatus == TimeTrackingStatus.IDLE) {
                    setStatus(TimeTrackingStatus.RUNNING, now, EVENT_FROM_READ);
                }
            }
        }
    }

    public void notifyUserStop() {
        long now = System.currentTimeMillis();
        readLastActivityMs = now;
        if (readStatus == TimeTrackingStatus.IDLE) {
            synchronized (this) {
                if (readStatus == TimeTrackingStatus.IDLE) {
                    setStatus(TimeTrackingStatus.STOPPED, now, EVENT_FROM_READ);
                    setStatus(TimeTrackingStatus.STOPPED, now, EVENT_FROM_WRITE);
                }
            }
        }
    }


    @Override
    public void dispose() {
        if (DEBUG_LIFECYCLE) {
            log.debug("[TimeTrack] disposeComponent: {}", this);
        }
        ALL_OPENED_TRACKERS.remove(this);
        if (readTicker != null) {
            readTicker.cancel(false);
            readTicker = null;
        }

        if (writeTicker != null) {
            writeTicker.cancel(false);
            writeTicker = null;
        }

        if (calculator != null) {
            calculator.cancel(false);
            calculator = null;
        }
        setStatus(TimeTrackingStatus.STOPPED, EVENT_FROM_WRITE);
        setStatus(TimeTrackingStatus.STOPPED, EVENT_FROM_READ);
        if (!pushBuffer.isEmpty()) {
            asyncBatchPushBuffer();
        }
    }

    @Override
    public @NotNull TimeTrackerPersistentState getState() {
        if (DEBUG_LIFECYCLE) {
            log.debug("[TimeTrack] getState() : {}", this);
        }
        TimeTrackerPersistentState result = new TimeTrackerPersistentState();
        result.totalWriteTimeSeconds = msToS(writeTotalTimeMs);
        result.totalReadTimeSeconds = msToS(readTotalTimeMs);

        result.autoStart = autoStart;
        result.idleThresholdMs = idleThresholdMs;

        result.writeAutoCountIdleSeconds = writeAutoCountIdleSeconds;
        result.readAutoCountIdleSeconds = readAutoCountIdleSeconds;

        result.stopWhenIdleRatherThanPausing = stopWhenIdleRatherThanPausing;
        result.pauseOtherTrackerInstances = pauseOtherTrackerInstances;
        result.projectName = project.getName();
        result.date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis()));

        result.writeTimeSecondsAfterLastCommit = writeTimeSecondsAfterLastCommit;
        result.readTimeSecondsAfterLastCommit = readTimeSecondsAfterLastCommit;
        return result;
    }

    @Override
    public void noStateLoaded() {
        if (DEBUG_LIFECYCLE) {
            log.debug("[TimeTrack] noStateLoaded() : {}", this);
        }
        loadState(DefaultSettingsService.getDefaultState());
    }

    public void loadStateDefaults(@NotNull TimeTrackerPersistentState defaults) {
        final TimeTrackerPersistentState modifiedState = getState();
        modifiedState.setDefaultsFrom(defaults);
        loadState(modifiedState);
    }

    @Override
    public void loadState(@NotNull TimeTrackerPersistentState state) {
        if (DEBUG_LIFECYCLE) {
            log.debug("[TimeTrack] loadState() : {}", this);
        }
        ApplicationManager.getApplication()
                .invokeLater(() -> {
                    synchronized (this) {
                        this.writeTotalTimeMs = state.totalWriteTimeSeconds * 1000L;
                        this.readTotalTimeMs = state.totalReadTimeSeconds * 1000L;
                        setAutoStart(state.autoStart);
                        setIdleThresholdMs(state.idleThresholdMs);
                        setWriteAutoCountIdleSeconds(state.writeAutoCountIdleSeconds);
                        setReadAutoCountIdleSeconds(state.readAutoCountIdleSeconds);
                        setStopWhenIdleRatherThanPausing(state.stopWhenIdleRatherThanPausing);
                        setPauseOtherTrackerInstances(state.pauseOtherTrackerInstances);
                        setReportDate(state.date);

                        setReadTimeSecondsAfterLastCommit(state.readTimeSecondsAfterLastCommit);
                        setWriteTimeSecondsAfterLastCommit(state.writeTimeSecondsAfterLastCommit);
                    }
                    repaintWidget(true);
                });
    }
}
