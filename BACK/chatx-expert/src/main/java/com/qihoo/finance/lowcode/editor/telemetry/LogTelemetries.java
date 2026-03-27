package com.qihoo.finance.lowcode.editor.telemetry;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import java.util.LinkedList;

@Getter
@Setter
public class LogTelemetries {

    LinkedList<LogTelemetry> logTelemetries = new LinkedList<>();

    Double contextualFilterScore = -1.0D;

    public void add(LogTelemetry logTelemetry) {
        if (this.logTelemetries.size() == 7) {
            SimpleRegression regression = new SimpleRegression();
            int i = 0;
            for (LogTelemetry el : this.logTelemetries)
                regression.addData(i++, el.getAccepted() ? 1.0D : 0.0D);
            this.contextualFilterScore = regression.predict(7.0D);
            this.logTelemetries.removeFirst();
        }
        this.logTelemetries.add(logTelemetry);
    }

    public void setAccepted(String taskId) {
        for (LogTelemetry el : this.logTelemetries) {
            if (el.getTaskId().equals(taskId)) {
                el.setAccepted(Boolean.TRUE);
                break;
            }
        }
    }

    public int countAccepted() {
        int count = 0;
        for (LogTelemetry logTelemetry : this.logTelemetries) {
            if (logTelemetry.getAccepted())
                count++;
        }
        return count;
    }
}
