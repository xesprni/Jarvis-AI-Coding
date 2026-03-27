package com.qihoo.finance.lowcode.editor.util;

public interface Cancellable {

    public static final Cancellable DUMB = new Cancellable() {
        public boolean isCancelled() {
            return false;
        }

        public void cancel() {}
    };

    boolean isCancelled();

    void cancel();
}
