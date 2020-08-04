package net.tjado.usbgadget;

import androidx.core.util.Consumer;

public class Log {

    protected static StringBuffer log = new StringBuffer();

    // error
    public static void e(String tag, String msg) {
        android.util.Log.e(tag, msg);
        internalLog(tag, msg);
    }

    public static void e(String tag, String msg, Throwable t) {
        android.util.Log.e(tag, msg, t);
        internalLog(tag, msg, t);
    }


    // debug
    public static void d(String tag, String msg) {
        android.util.Log.d(tag, msg);
        internalLog(tag, msg);
    }

    public static void d(String tag, String msg, Throwable t) {
        android.util.Log.d(tag, msg, t);
        internalLog(tag, msg, t);
    }


    // warning
    public static void w(String tag, String msg) {
        android.util.Log.w(tag, msg);
        internalLog(tag, msg);
    }

    public static void w(String tag, String msg, Throwable t) {
        android.util.Log.w(tag, msg, t);
        internalLog(tag, msg, t);
    }

    protected static void internalLog(String tag, String msg) {
        internalLog(tag, msg, null);
    }

    protected static void internalLog(String tag, String msg, Throwable t) {
        log.append(String.format("%s - %s", tag, msg));
        if (t != null) {
            log.append(t.getStackTrace().toString());
        }
        log.append(System.getProperty("line.separator"));
    }

    public static String getLog() {
        return log.toString();
    }
}
