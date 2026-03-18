package com.cashier.common.exception;

import com.cashier.common.result.ResultCode;
import lombok.Getter;

/**
 * 自定义业务异常
 * 用于在业务逻辑中抛出明确的错误信息
 *
 * @author cashier
 * @since 2024-01-01
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }
}
