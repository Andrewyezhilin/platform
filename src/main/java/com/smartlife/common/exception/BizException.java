package com.smartlife.common.exception;

/**
 * 业务异常。
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
