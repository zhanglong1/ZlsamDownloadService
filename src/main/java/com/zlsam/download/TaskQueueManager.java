package com.zlsam.download;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.text.TextUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zlsam.download.common.Common;
import com.zlsam.download.common.SmartLog;
import com.zlsam.download.model.DownloadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhanglong on 8/29/2016.
 */
public class TaskQueueManager {
    public static final int ERROR_INVALID_PARAMETER = -1;
    public static final int ERROR_OVERSTEP_MAX_LENGTH = -2;
    public static final int ERROR_FAILED_PERSIST = -3;
    public static final int ERROR_NOT_FOUND = -4;
    public static final int SUCCEED = 1;

    private static TaskQueueManager sInstance;
    private static Context sContext;

    // waiting, downloading, succeed and failed task queues.

    /** Shared */
    private List<DownloadTask> mWaitingQueue;

    /** Shared */
    private List<DownloadTask> mProcessingQueue;

    /** Shared */
    private List<DownloadTask> mSucceedQueue;

    /** Shared */
    private List<DownloadTask> mFailedQueue;

    private static final int WAITING_QUEUE_MAX_LENGTH = 20;
    private static final int PROCESSING_QUEUE_MAX_LENGTH = 3;
    private static final int SUCCEED_QUEUE_MAX_LENGTH = 20;
    private static final int FAILED_QUEUE_MAX_LENGTH = 20;

    private SharedPreferences mSp;
    private static final String KEY_WAITING_QUEUE = "key_waiting_queue";
    private static final String KEY_PROCESSING_QUEUE = "key_processing_queue";
    private static final String KEY_SUCCEED_QUEUE = "key_succeed_queue";
    private static final String KEY_FAILED_QUEUE = "key_failed_queue";

    private Gson mGson;

    /**
     * Must be called before you calling other methods.
     * @param context
     */
    public static void init(Context context) {
        sContext = context;
    }

    /**
     * You should call {@link #init(Context)} before calling this methods.
     * @return
     */
    public static TaskQueueManager getInstance() {
        if (null == sInstance) {
            sInstance = new TaskQueueManager();
        }
        return sInstance;
    }

    private TaskQueueManager() {
        if (null == sContext) {
            throw new IllegalStateException("context can't be found, have you called init()?");
        }
        mWaitingQueue = new ArrayList<>();
        mProcessingQueue = new ArrayList<>();
        mSucceedQueue = new ArrayList<>();
        mFailedQueue = new ArrayList<>();
        mSp = sContext.getSharedPreferences(sContext.getPackageName() + ":DownloadingTask", Context.MODE_PRIVATE);
        mGson = new Gson();

        // Try to recover from persistent
        String waitingQueueJson = mSp.getString(KEY_WAITING_QUEUE, null);
        if (!TextUtils.isEmpty(waitingQueueJson)) {
            mWaitingQueue.addAll((List) mGson.fromJson(waitingQueueJson, new TypeToken<ArrayList<DownloadTask>>() {}.getType()));
        }
        String processingQueueJson = mSp.getString(KEY_PROCESSING_QUEUE, null);
        if (!TextUtils.isEmpty(processingQueueJson)) {
            mProcessingQueue.addAll((List) mGson.fromJson(processingQueueJson, new TypeToken<ArrayList<DownloadTask>>() {}.getType()));
        }
        String succeedQueueJson = mSp.getString(KEY_SUCCEED_QUEUE, null);
        if (!TextUtils.isEmpty(succeedQueueJson)) {
            mSucceedQueue.addAll((List) mGson.fromJson(succeedQueueJson, new TypeToken<ArrayList<DownloadTask>>() {}.getType()));
        } else {
            // Clear cached files.
            String cacheDir = Environment.getExternalStorageDirectory().getPath() + "/Zlsam/Download/";
            File dir = new File(cacheDir);
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (null != files) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
            SmartLog.i(Common.COMMON_LOG_TAG, "No task was found in shared preference, we cleared the download cache directory: " + cacheDir);
        }
        String failedQueueJson = mSp.getString(KEY_FAILED_QUEUE, null);
        if (!TextUtils.isEmpty(failedQueueJson)) {
            mFailedQueue.addAll((List) mGson.fromJson(failedQueueJson, new TypeToken<ArrayList<DownloadTask>>() {}.getType()));
        }

        // Re-waiting interrupted processing tasks
        if (!mProcessingQueue.isEmpty()) {
            mWaitingQueue.addAll(mProcessingQueue);
            mProcessingQueue.clear();
        }

        SmartLog.i(Common.COMMON_LOG_TAG, "Task queue is initialized, " + currentState2String() + ".");
    }

    public DownloadTask queryAll(String url) {
        DownloadTask task = queryInWaitingQueue(url);
        task = null == task ? queryInProcessingQueue(url) : task;
        task = null == task ? queryInSucceedQueue(url) : task;
        return null == task ? queryInFailedQueue(url) : task;
    }

    public static String errCode2ErrMsg(int code) {
        switch (code) {
            case ERROR_INVALID_PARAMETER:
                return "invalid argument(s)";
            case ERROR_OVERSTEP_MAX_LENGTH:
                return "thread pool is full";
            case ERROR_FAILED_PERSIST:
                return "failed to persist";
            case ERROR_NOT_FOUND:
                return "task was not found";
            default:
                return "unknown error";
        }
    }

    public DownloadTask queryInWaitingQueue(String url) {
        for (DownloadTask task : mWaitingQueue) {
            if (task.url.equals(url.trim())) {
                return task;
            }
        }
        return null;
    }

    public DownloadTask queryInProcessingQueue(String url) {
        for (DownloadTask task : mProcessingQueue) {
            if (task.url.equals(url.trim())) {
                return task;
            }
        }
        return null;
    }

    public DownloadTask queryInSucceedQueue(String url) {
        for (DownloadTask task : mSucceedQueue) {
            if (task.url.equals(url.trim())) {
                return task;
            }
        }
        return null;
    }

    public DownloadTask queryInFailedQueue(String url) {
        for (DownloadTask task : mFailedQueue) {
            if (task.url.equals(url.trim())) {
                return task;
            }
        }
        return null;
    }

    /**
     *
     * @param task
     * @return 1 succeed, -1 invalid parameter, -2 max length, -3 failed to persist
     */
    public int add2WaitingQueue(DownloadTask task, boolean jumpQueue) {
        if (null == task) {
            return ERROR_INVALID_PARAMETER;
        }

        // Check if already exist
        DownloadTask alreadyTask = queryAll(task.url);
        if (null != alreadyTask) {
            switch (alreadyTask.state) {
                case DownloadTask.STATE_DOWNLOADED:
                    BroadcastUtil.sendDownloadSucceedBroadcast(sContext, alreadyTask.url, alreadyTask.absolutePath);
                    return SUCCEED;
                case DownloadTask.STATE_FAILED:
                    deleteFromFailedQueue(alreadyTask);
                    break;
                case DownloadTask.STATE_DOWNLOADING:
                case DownloadTask.STATE_WAITING:
                    return SUCCEED;
                default:
                    SmartLog.w(Common.COMMON_LOG_TAG, "Unknown task state! task: " + mGson.toJson(alreadyTask));
                    break;
            }
        }

        // Check if overstep max length
        if (mWaitingQueue.size() >= WAITING_QUEUE_MAX_LENGTH) {
            return ERROR_OVERSTEP_MAX_LENGTH;
        }

        // Now add it
        task.state = DownloadTask.STATE_WAITING;
        if (jumpQueue) {
            mWaitingQueue.add(task);
        } else {
            mWaitingQueue.add(0, task);
        }
        if (!mSp.edit().putString(KEY_WAITING_QUEUE, mGson.toJson(mWaitingQueue)).commit()) {
            mWaitingQueue.remove(0);
            return ERROR_FAILED_PERSIST;
        }

        // Succeed
        SmartLog.i(Common.COMMON_LOG_TAG, "Added a new task to waiting queue, current state: " + currentState2String() + ".");
        return SUCCEED;
    }

    /**
     *
     * @return The next waiting task if not empty, or null if empty.
     */
    public DownloadTask getNextWaitingTask() {
        if (mWaitingQueue.size() <= 0) {
            return null;
        }
        return mWaitingQueue.get(mWaitingQueue.size() - 1);
    }

    /**
     *
     * @param task
     * @return 1 succeed, -1 invalid parameter, -3 failed to persist
     */
    public int deleteFromWaitingQueue(DownloadTask task) {
        if (null == task) {
            return ERROR_INVALID_PARAMETER;
        }
        for (int i = 0; i < mWaitingQueue.size(); i++) {
            DownloadTask waitingTask = mWaitingQueue.get(i);
            if (waitingTask.url.equals(task.url.trim())) {
                mWaitingQueue.remove(waitingTask);
                if (!mSp.edit().putString(KEY_WAITING_QUEUE, mGson.toJson(mWaitingQueue)).commit()) {
                    mWaitingQueue.add(i, waitingTask);
                    return ERROR_FAILED_PERSIST;
                } else {
                    SmartLog.i(Common.COMMON_LOG_TAG, "Deleted a task from waiting queue, current state: " + currentState2String() + ".");
                    return SUCCEED;
                }
            }
        }
        return ERROR_NOT_FOUND;
    }

    public int clearWaitingQueue() {
        mWaitingQueue.clear();
        if (!mSp.edit().putString(KEY_WAITING_QUEUE, mGson.toJson(mWaitingQueue)).commit()) {
            // Restore
            String waitingQueueJson = mSp.getString(KEY_WAITING_QUEUE, null);
            if (!TextUtils.isEmpty(waitingQueueJson)) {
                mWaitingQueue.addAll((List) mGson.fromJson(waitingQueueJson, new TypeToken<ArrayList<DownloadTask>>() {}.getType()));
            }
            return ERROR_FAILED_PERSIST;
        } else {
            SmartLog.i(Common.COMMON_LOG_TAG, "Cleared waiting queue, current state: " + currentState2String() + ".");
            return SUCCEED;
        }
    }

    public int add2ProcessingQueue(DownloadTask task) {
        if (null == task) {
            return ERROR_INVALID_PARAMETER;
        }

        // Check if already exist
        for (DownloadTask processingTask : mProcessingQueue) {
            if (processingTask.url.equals(task.url.trim())) {
                // Already exist, we do nothing
                return SUCCEED;
            }
        }

        // Check if overstep max length
        if (mProcessingQueue.size() >= PROCESSING_QUEUE_MAX_LENGTH) {
            return ERROR_OVERSTEP_MAX_LENGTH;
        }

        // Now add it
        task.state = DownloadTask.STATE_DOWNLOADING;
        mProcessingQueue.add(0, task);
        if (!mSp.edit().putString(KEY_PROCESSING_QUEUE, mGson.toJson(mProcessingQueue)).commit()) {
            mProcessingQueue.remove(0);
            return ERROR_FAILED_PERSIST;
        }

        // Succeed
        SmartLog.i(Common.COMMON_LOG_TAG, "Added a new task to processing queue, current state: " + currentState2String() + ".");
        return SUCCEED;
    }

    public int deleteFromProcessingQueue(DownloadTask task) {
        if (null == task) {
            return ERROR_INVALID_PARAMETER;
        }
        for (int i = 0; i < mProcessingQueue.size(); i++) {
            DownloadTask processingTask = mProcessingQueue.get(i);
            if (processingTask.url.equals(task.url.trim())) {
                mProcessingQueue.remove(processingTask);
                if (!mSp.edit().putString(KEY_PROCESSING_QUEUE, mGson.toJson(mProcessingQueue)).commit()) {
                    mProcessingQueue.add(i, processingTask);
                    return ERROR_FAILED_PERSIST;
                } else {
                    SmartLog.i(Common.COMMON_LOG_TAG, "Delete a task from processing queue, current state: " + currentState2String() + ".");
                    return SUCCEED;
                }
            }
        }
        return ERROR_NOT_FOUND;
    }

    /**
     * If overstep the max length, the oldest one will be deleted.
     * @param task
     * @return
     */
    public int add2SucceedQueue(DownloadTask task) {
        if (null == task) {
            return ERROR_INVALID_PARAMETER;
        }

        // Check if already exist
        for (DownloadTask succeedTask : mSucceedQueue) {
            if (succeedTask.url.equals(task.url.trim())) {
                // Already exist, we do nothing
                return SUCCEED;
            }
        }

        // If overstep max length, clear the last one
        if (mSucceedQueue.size() >= SUCCEED_QUEUE_MAX_LENGTH) {
            DownloadTask oldestTask = mSucceedQueue.get(mSucceedQueue.size() - 1);
            File file = new File(oldestTask.absolutePath);
            if (file.exists()) {
                file.delete();
            }
            mSucceedQueue.remove(mSucceedQueue.size() - 1);
            BroadcastUtil.sendClearOneBroadcast(sContext, oldestTask.url);
        }

        // Now add it
        task.state = DownloadTask.STATE_DOWNLOADED;
        mSucceedQueue.add(0, task);
        if (!mSp.edit().putString(KEY_SUCCEED_QUEUE, mGson.toJson(mSucceedQueue)).commit()) {
            mSucceedQueue.remove(0);
            return ERROR_FAILED_PERSIST;
        }

        // Succeed
        SmartLog.i(Common.COMMON_LOG_TAG, "Added a new task to succeed queue, current state: " + currentState2String() + ".");
        return SUCCEED;
    }

    public int deleteFromSucceedQueue(DownloadTask task) {
        if (null == task) {
            return ERROR_INVALID_PARAMETER;
        }
        for (int i = 0; i < mSucceedQueue.size(); i++) {
            DownloadTask succeedTask = mSucceedQueue.get(i);
            if (succeedTask.url.equals(task.url.trim())) {
                mSucceedQueue.remove(succeedTask);
                if (!mSp.edit().putString(KEY_PROCESSING_QUEUE, mGson.toJson(mSucceedQueue)).commit()) {
                    mSucceedQueue.add(i, succeedTask);
                    return ERROR_FAILED_PERSIST;
                } else {
                    File file = new File(succeedTask.absolutePath);
                    if (file.exists()) {
                        file.delete();
                    }
                    BroadcastUtil.sendClearOneBroadcast(sContext, succeedTask.url);
                    SmartLog.i(Common.COMMON_LOG_TAG, "Deleted a task from succeed queue, current state: " + currentState2String() + ".");
                    return SUCCEED;
                }
            }
        }
        return ERROR_NOT_FOUND;
    }

    public int add2FailedQueue(DownloadTask task) {
        if (null == task) {
            return ERROR_INVALID_PARAMETER;
        }

        // Check if already exist
        for (DownloadTask failedTask : mFailedQueue) {
            if (failedTask.url.equals(task.url.trim())) {
                // Already exist, we do nothing
                return SUCCEED;
            }
        }

        // If overstep max length, clear the oldest one
        if (mFailedQueue.size() >= FAILED_QUEUE_MAX_LENGTH) {
            DownloadTask lastTask = mFailedQueue.get(mFailedQueue.size() - 1);
            if (!TextUtils.isEmpty(lastTask.absolutePath)) {
                File file = new File(lastTask.absolutePath);
                if (file.exists()) {
                    file.delete();
                }
            }
            mFailedQueue.remove(mFailedQueue.size() - 1);
            BroadcastUtil.sendClearOneBroadcast(sContext, lastTask.url);
        }

        // Now add it
        task.state = DownloadTask.STATE_FAILED;
        mFailedQueue.add(0, task);
        if (!mSp.edit().putString(KEY_FAILED_QUEUE, mGson.toJson(mFailedQueue)).commit()) {
            mFailedQueue.remove(0);
            return ERROR_FAILED_PERSIST;
        }

        // Succeed
        SmartLog.i(Common.COMMON_LOG_TAG, "Added a new task to failed queue, current state: " + currentState2String() + ".");
        return SUCCEED;
    }

    public int deleteFromFailedQueue(DownloadTask task) {
        if (null == task) {
            return ERROR_INVALID_PARAMETER;
        }
        for (int i = 0; i < mFailedQueue.size(); i++) {
            DownloadTask failedTask = mFailedQueue.get(i);
            if (failedTask.url.equals(task.url.trim())) {
                mFailedQueue.remove(failedTask);
                if (!mSp.edit().putString(KEY_PROCESSING_QUEUE, mGson.toJson(mFailedQueue)).commit()) {
                    mFailedQueue.add(i, failedTask);
                    return ERROR_FAILED_PERSIST;
                } else {
                    if (!TextUtils.isEmpty(failedTask.absolutePath)) {
                        File file = new File(failedTask.absolutePath);
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                    BroadcastUtil.sendClearOneBroadcast(sContext, failedTask.url);
                    SmartLog.i(Common.COMMON_LOG_TAG, "Deleted a task from failed queue, current state: " + currentState2String() + ".");
                    return SUCCEED;
                }
            }
        }
        return ERROR_NOT_FOUND;
    }

    private String currentState2String() {
        if (BuildConfig.DEBUG) {
            return "waiting queue: " + mGson.toJson(mWaitingQueue)
                    + ", processing queue: " + mGson.toJson(mProcessingQueue) + ", succeed queue: " + mGson.toJson(mSucceedQueue)
                    + ", failed queue: " + mGson.toJson(mFailedQueue);
        } else {
            return "waiting queue: " + mWaitingQueue.size() +
                    ", processing queue: " + mProcessingQueue.size() + ", succeed queue: " + mSucceedQueue.size() + ", failed queue: " +
                    mFailedQueue.size();
        }
    }
}
