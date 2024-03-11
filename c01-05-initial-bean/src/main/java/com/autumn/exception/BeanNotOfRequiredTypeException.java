package com.autumn.exception;

/**
 * @author huangcanjie
 */
public class BeanNotOfRequiredTypeException extends BeansException{
    public BeanNotOfRequiredTypeException() {
    }

    public BeanNotOfRequiredTypeException(String message) {
        super(message);
    }
}
