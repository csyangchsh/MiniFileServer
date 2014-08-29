package com.csyangchsh.fs;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author csyangchsh
 * Date: 14/8/29
 */
public abstract class Util {

    public static final void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }
}
