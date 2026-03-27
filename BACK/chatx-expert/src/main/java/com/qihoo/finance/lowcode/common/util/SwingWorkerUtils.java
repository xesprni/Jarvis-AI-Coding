package com.qihoo.finance.lowcode.common.util;

import lombok.SneakyThrows;

import javax.swing.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * SwingWorkerUtils
 *
 * @author fengjinfu-jk
 * date 2023/12/4
 * @version 1.0.0
 * @apiNote SwingWorkerUtils
 */
public class SwingWorkerUtils {
    private final static ExecutorService executorService = Executors.newFixedThreadPool(1);

    public static <T> void execute(Supplier<T> doInBackground, Consumer<T> doInUiThread) {
        execute(doInBackground, doInUiThread, true);
    }

    public static <T> void execute(Supplier<T> doInBackground, Consumer<T> doInUiThread, boolean async) {
        if (!async) {
            doInUiThread.accept(doInBackground.get());
            return;
        }

        new SwingWorker<T, T>() {
            @Override
            protected T doInBackground() {
                return doInBackground.get();
            }

            @SneakyThrows
            @Override
            protected void done() {
                doInUiThread.accept(get());
                super.done();
            }
        }.execute();
    }

    public static void doInBackground(Runnable runnable) {
        executorService.execute(runnable);
    }
}
