package com.autumn.exception;

/**
 * @author huangcanjie
 */
public class BeansException extends NestedRuntimeException{

    public BeansException() {
    }

    public BeansException(String message) {
        super(message);
    }

    public BeansException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeansException(Throwable cause) {
        super(cause);
    }
}
