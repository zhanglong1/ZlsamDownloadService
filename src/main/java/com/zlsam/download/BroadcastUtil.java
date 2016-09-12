package com.zlsam.download;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.zlsam.download.common.Common;
import com.zlsam.download.common.SmartLog;

/**
 * Created by zhanglong on 8/31/2016.
 */
public class BroadcastUtil {
    private static final String SUFFIX_ACTION_DOWNLOAD_SUCCEED = ".action.DOWNLOAD_SUCCEED";
    private static final String SUFFIX_ACTION_DOWNLOAD_FAILED = ".action.DOWNLOAD_FAILED";
    private static final String SUFFIX_ACTION_CLEAR_ONE = ".action.CLEAR_ONE";

    /**
     *
     * @param context Should not be null, or nothing will be done but logging the error.
     * @param url Should not be null, or nothing will be done but logging the error.
     * @param absolutePath Should not be null, or nothing will be done but logging the error.
     */
    public static void sendDownloadSucceedBroadcast(Context context, String url, String absolutePath) {
        if (null == context || TextUtils.isEmpty(url) || TextUtils.isEmpty(absolutePath)) {
            String errMsg = "Failed to send download-succeed broadcast, invalid parameters, context, url and absolutePath should not be null or empty, real value: context: " +
                    (null == context ? "null" : "non-null") + ", url: " + url + ", absolutePath: " + absolutePath + ".";
            if (BuildConfig.DEBUG) {
                throw new IllegalArgumentException(errMsg);
            } else {
                SmartLog.e(Common.COMMON_LOG_TAG, errMsg);
                return;
            }
        }
        Intent intent = new Intent(context.getPackageName() + SUFFIX_ACTION_DOWNLOAD_SUCCEED);
        intent.putExtra("url", url);
        intent.putExtra("absolutePath", absolutePath);
        context.sendBroadcast(intent);
    }

    /**
     *
     * @param context Should not be null, or nothing will be done but logging the error.
     * @param url Should not be null, or nothing will be done but logging the error.
     */
    public static void sendDownloadFailedBroadcast(Context context, String url) {
        if (null == context || TextUtils.isEmpty(url)) {
            String errMsg = "Failed to send download-failed broadcast, invalid parameters, context, url should not be null or empty, real value: context: " +
                    (null == context ? "null" : "non-null") + ", url: " + url + ".";
            if (BuildConfig.DEBUG) {
                throw new IllegalArgumentException(errMsg);
            } else {
                SmartLog.e(Common.COMMON_LOG_TAG, errMsg);
                return;
            }
        }
        Intent intent = new Intent(context.getPackageName() + SUFFIX_ACTION_DOWNLOAD_FAILED);
        intent.putExtra("url", url);
        context.sendBroadcast(intent);
    }

    /**
     *
     * @param context Should not be null, or nothing will be done but logging the error.
     * @param url Should not be null, or nothing will be done but logging the error.
     */
    public static void sendClearOneBroadcast(Context context, String url) {
        if (null == context || TextUtils.isEmpty(url)) {
            String errMsg = "Failed to send clear-one broadcast, invalid parameters, context, url should not be null or empty, real value: context: " +
                    (null == context ? "null" : "non-null") + ", url: " + url + ".";
            if (BuildConfig.DEBUG) {
                throw new IllegalArgumentException(errMsg);
            } else {
                SmartLog.e(Common.COMMON_LOG_TAG, errMsg);
                return;
            }
        }
        Intent intent = new Intent(context.getPackageName() + SUFFIX_ACTION_CLEAR_ONE);
        intent.putExtra("url", url);
        context.sendBroadcast(intent);
    }
}
