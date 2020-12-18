package net.tjado.usbgadget;

public class Log {

    protected static StringBuffer log = new StringBuffer();
    protected static OnLogChangeListener onLogChangeListener;

    public interface OnLogChangeListener{
        void onLogChanged();
    }

    public void setOnDataChangeListener(OnLogChangeListener onLogChangeListener){
        onLogChangeListener = onLogChangeListener;
    }

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

        if(onLogChangeListener != null){
            onLogChangeListener.onLogChanged();
        }
    }

    public static String getLog() {
        return log.toString();
    }
}
