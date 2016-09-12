package com.zlsam.download.common;

/**
 * Each log will be appended with class name, method name, and line number.
 * <br/><br/>
 * Created by zhanglong on 15/5/5.
 */
public class SmartLog {

    private static boolean mDebuggerable = true;

    public static boolean isDebuggable() {
        return mDebuggerable;
    }

    public static void setDebuggerable(boolean value) {
        mDebuggerable = value;
    }

    private static String createLog(Env env, String log){

        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        buffer.append(env.mClassName);
        buffer.append(":");
        buffer.append(env.mMethodName);
        buffer.append(":");
        buffer.append(env.mLineNumber);
        buffer.append("]");
        buffer.append(log);

        return buffer.toString();
    }

    private static class Env {
        public String mClassName;
        public String mMethodName;
        public int mLineNumber;
    }

    private static Env getMethodNames(StackTraceElement[] sElements){
        Env env = new Env();
        env.mClassName = sElements[1].getClassName().split("\\$")[0];
        env.mMethodName = sElements[1].getMethodName();
        env.mLineNumber = sElements[1].getLineNumber();
        return env;
    }

    public static void v(String message) {
        if (!isDebuggable()) return;

        Env env = getMethodNames(new Throwable().getStackTrace());
        String[] classNameSplits = env.mClassName.split("\\.");
        android.util.Log.v(classNameSplits[classNameSplits.length - 1], createLog(env, message));
    }

    public static void v(String tag, String message) {
        if (!isDebuggable()) return;

        Env env = getMethodNames(new Throwable().getStackTrace());
        android.util.Log.v(tag, createLog(env, message));
    }

    public static void d(String message){
        if (!isDebuggable()) return;

        Env env = getMethodNames(new Throwable().getStackTrace());
        String[] classNameSplits = env.mClassName.split("\\.");
        android.util.Log.d(classNameSplits[classNameSplits.length - 1], createLog(env, message));
    }

    public static void d(String tag, String message) {
        if (!isDebuggable()) return;

        Env env = getMethodNames(new Throwable().getStackTrace());
        android.util.Log.d(tag, createLog(env, message));
    }

    public static void i(String message){
        Env env = getMethodNames(new Throwable().getStackTrace());
        String[] classNameSplits = env.mClassName.split("\\.");
        android.util.Log.i(classNameSplits[classNameSplits.length - 1], createLog(env, message));
    }

    public static void i(String tag, String message) {
        Env env = getMethodNames(new Throwable().getStackTrace());
        android.util.Log.i(tag, createLog(env, message));
    }

    public static void w(String message){
        Env env = getMethodNames(new Throwable().getStackTrace());
        String[] classNameSplits = env.mClassName.split("\\.");
        android.util.Log.w(classNameSplits[classNameSplits.length - 1], createLog(env, message));
    }

    public static void w(String tag, String message) {
        Env env = getMethodNames(new Throwable().getStackTrace());
        android.util.Log.w(tag, createLog(env, message));
    }

    public static void e(String message){
        Env env = getMethodNames(new Throwable().getStackTrace());
        String[] classNameSplits = env.mClassName.split("\\.");
        android.util.Log.e(classNameSplits[classNameSplits.length - 1], createLog(env, message));
    }

    public static void e(String tag, String message) {
        Env env = getMethodNames(new Throwable().getStackTrace());
        android.util.Log.e(tag, createLog(env, message));
    }

    public static void wtf(String message){
        Env env = getMethodNames(new Throwable().getStackTrace());
        String[] classNameSplits = env.mClassName.split("\\.");
        android.util.Log.wtf(classNameSplits[classNameSplits.length - 1], createLog(env, message));
    }

    public static void wtf(String tag, String message) {
        Env env = getMethodNames(new Throwable().getStackTrace());
        android.util.Log.wtf(tag, createLog(env, message));
    }

}
