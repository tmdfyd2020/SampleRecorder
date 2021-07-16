package org.techtown.samplerecorder;

public class myLog {
    static final String TAG = "[MZ]";

    public static void v(String message) {
        android.util.Log.v(TAG, buildLogMsg(message));
    }

    public static void d(String message) {
        android.util.Log.d(TAG, buildLogMsg(message));
    }

    public static void i(String message) {
        android.util.Log.i(TAG, buildLogMsg(message));
    }

    public static void w(String message) {
        android.util.Log.w(TAG, buildLogMsg(message));
    }

    public static void e(String message) {
        android.util.Log.e(TAG, buildLogMsg(message));
    }

    public static String buildLogMsg(String message) {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[4];
        StringBuilder sb = new StringBuilder();
        sb.append("["); sb.append(ste.getFileName().replace(".java", "")); sb.append("] ");
        sb.append("["); sb.append(ste.getMethodName()); sb.append("()] ");
        sb.append("["); sb.append(message); sb.append("]");
        return sb.toString();
    }
}
