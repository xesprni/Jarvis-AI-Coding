package com.qihoo.finance.lowcode.aiquestion.exception;

public class RemoteApiException  extends RuntimeException {
    public RemoteApiException() {
    }

    public RemoteApiException(String message) {
        super(message);
    }

    public RemoteApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public RemoteApiException(Throwable cause) {
        super(cause);
    }

    public RemoteApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
