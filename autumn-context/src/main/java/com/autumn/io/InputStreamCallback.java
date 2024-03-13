package com.autumn.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author huangcanjie
 */
@FunctionalInterface
public interface InputStreamCallback<T> {

    T doWithInputStream(InputStream stream) throws IOException;
}
