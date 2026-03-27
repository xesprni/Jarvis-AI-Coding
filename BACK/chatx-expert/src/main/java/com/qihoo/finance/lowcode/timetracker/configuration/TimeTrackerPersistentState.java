package com.qihoo.finance.lowcode.timetracker.configuration;

/**
 * @author weiyichao
 * @date 2023-07-21
 **/
public class TimeTrackerPersistentState {

    public long writeTimeSecondsAfterLastCommit = 0;
    public long readTimeSecondsAfterLastCommit = 0;

    public long totalWriteTimeSeconds = 0;

    public long totalReadTimeSeconds = 0;

    public boolean autoStart = true;
    public long idleThresholdMs = 2 * 1000;
    public int autoCountIdleSeconds = 5;

    public int readAutoCountIdleSeconds = 120;

    public int writeAutoCountIdleSeconds = 60;

    public boolean stopWhenIdleRatherThanPausing = false;
    public boolean pauseOtherTrackerInstances = true;


    public String ideTimePattern = DEFAULT_IDE_TIME_PATTERN;
    public String gitTimePattern = DEFAULT_GIT_TIME_PATTERN;

    public String projectName;

    public String date;

    public void setDefaultsFrom(final TimeTrackerPersistentState state) {
        this.autoStart = state.autoStart;
        this.idleThresholdMs = state.idleThresholdMs;
        this.autoCountIdleSeconds = state.autoCountIdleSeconds;
        this.stopWhenIdleRatherThanPausing = state.stopWhenIdleRatherThanPausing;
        this.pauseOtherTrackerInstances = state.pauseOtherTrackerInstances;

        this.ideTimePattern = state.ideTimePattern;
        this.gitTimePattern = state.gitTimePattern;

        this.date = state.date;
        this.projectName = state.projectName;
        this.writeAutoCountIdleSeconds = state.writeAutoCountIdleSeconds;
        this.readAutoCountIdleSeconds = state.readAutoCountIdleSeconds;
    }

    public static String DEFAULT_IDE_TIME_PATTERN = "{{lh \"hr\"s}} {{lm \"min\"}} {{ts \"sec\"}}";
    public static String DEFAULT_GIT_TIME_PATTERN =
            "Took {{lh \"hour\"s}} {{lm \"minute\"s}} {{ts \"second\"s}}";
}
