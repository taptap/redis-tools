package com.taptap.github.redistools.utils;


import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/11/20
 */
public class ExceptionUtils {
    public static String readStackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            throwable.printStackTrace(printWriter);
        }
        return stringWriter.toString();
    }
}
