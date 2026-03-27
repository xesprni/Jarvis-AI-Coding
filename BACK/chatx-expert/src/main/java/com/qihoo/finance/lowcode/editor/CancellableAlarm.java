package com.qihoo.finance.lowcode.editor;

import com.intellij.openapi.Disposable;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class CancellableAlarm {
    private final Object LOCK = new Object();

    private final Alarm alarm;

    CancellableAlarm(@NotNull Disposable parentDisposable) {
        this.alarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, parentDisposable);
    }

    void cancelAllRequests() {
        synchronized (this.LOCK) {
            this.alarm.cancelAllRequests();
        }
    }

    void cancelAllAndAddRequest(@NotNull Runnable request, int delayMillis) {
        synchronized (this.LOCK) {
            this.alarm.cancelAllRequests();
            this.alarm.addRequest(request, delayMillis);
        }
    }

    @TestOnly
    void waitForAllExecuted(int timeout, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
        synchronized (this.LOCK) {
            this.alarm.waitForAllExecuted(timeout, timeUnit);
        }
    }
}
