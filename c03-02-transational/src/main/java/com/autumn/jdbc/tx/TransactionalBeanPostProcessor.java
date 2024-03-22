package com.autumn.jdbc.tx;

import com.autumn.annotation.Transactional;
import com.autumn.aop.AnnotationProxyBeanPostProcessor;

/**
 * @author huangcanjie
 */
public class TransactionalBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Transactional> {
}
