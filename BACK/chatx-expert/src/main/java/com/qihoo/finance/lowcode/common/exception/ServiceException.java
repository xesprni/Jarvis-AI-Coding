package com.qihoo.finance.lowcode.common.exception;

import com.qihoo.finance.lowcode.common.constants.ServiceErrorCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * @author yangzhihao
 * @date 2023/8/14
 */
@Getter
public class ServiceException extends RuntimeException {
    protected final String errorCode;
    protected final String message;

    public ServiceException(ServiceErrorCode serviceErrorCode) {
        super(serviceErrorCode.getMessage());
        this.errorCode = serviceErrorCode.getCode();
        this.message = serviceErrorCode.getMessage();
    }

    public ServiceException(ServiceErrorCode serviceErrorCode, String errMsg) {
        super(serviceErrorCode.getMessage());
        this.errorCode = serviceErrorCode.getCode();
        this.message = StringUtils.isBlank(errMsg) ? serviceErrorCode.getMessage() : errMsg;
    }

    public ServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.message = message;
    }

    public ServiceException(String errorCode, String message, Throwable throwable) {
        super(message, throwable);
        this.errorCode = errorCode;
        this.message = message;
    }

}
