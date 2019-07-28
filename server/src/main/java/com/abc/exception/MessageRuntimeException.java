package com.abc.exception;

public class MessageRuntimeException extends RuntimeException {

    public MessageRuntimeException(String msg) {
        super(msg);
    }

    public MessageRuntimeException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
