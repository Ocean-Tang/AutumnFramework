package com.autumn.exception;

/**
 * @author huangcanjie
 */
public class BeanCreationException extends BeansException{

    public BeanCreationException() {
    }

    public BeanCreationException(String message) {
        super(message);
    }

    public BeanCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanCreationException(Throwable cause) {
        super(cause);
    }
}
