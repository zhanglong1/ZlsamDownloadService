package com.zlsam.download;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import com.zlsam.download.common.Common;
import com.zlsam.download.common.SmartLog;
import com.zlsam.download.model.DownloadTask;

/**
 * <p>This is the main downloading service that is responsible for receiving downloading requests and dispatching
 * them to the working thread pool.</p>
 *
 * <p>It also manages some other sub-modules, such as persistent, requestQueue, and space management.</p>
 *
 * Created by zhanglong on 8/25/2016.
 */
public class MainDownloadingService extends Service {

    /** Return this instance when a client request to bind. */
    private RemoteInterface mRemoteInterface;

    /**
     * <p>The downloading task queue.</p>
     *
     * <p>Note: it's shared and must be thread-safe</p>
     */
    private TaskQueueManager mTaskQueueManager;

    private TaskDispatcher mTaskDispatcher;

    private static final String SP_KEY_TASK_QUEUE = "sp_key_task_queue";

    @Override
    public void onCreate() {
        super.onCreate();
        init();
    }

    // For on-demand mode
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    // For bind mode
    @Override
    public IBinder onBind(Intent intent) {
        return mRemoteInterface;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        clear();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }
////////////////////////////////////// private methods /////////////////////////////////////////

    private void init() {
        SmartLog.setDebuggerable(BuildConfig.DEBUG);
        mRemoteInterface = new RemoteInterface();
        TaskQueueManager.init(this);
        mTaskQueueManager = TaskQueueManager.getInstance();
        TaskDispatcher.init(this);
        mTaskDispatcher = TaskDispatcher.getInstance();
        mTaskDispatcher.start();
        SmartLog.i(Common.COMMON_LOG_TAG, "ZlsamDownloadService was initialized.");
    }

    private void clear() {
        mTaskDispatcher.stop();
        SmartLog.i(Common.COMMON_LOG_TAG, "ZlsamDownloadService was closed.");
    }

    ////////////////////////////////////// inner class /////////////////////////////////////////////

    /**
     * All public methods in this class are thread-safe.
     */
    private class RemoteInterface extends IMainDownloadingService.Stub {

        @Override
        public int add2Queue(String url, String checkSum, String desiredName, boolean jumpQueue) throws RemoteException {
            // Check parameters
            if (TextUtils.isEmpty(url)) {
                throw new RemoteException("url should not be null or empty!");
            }

            // Prepare data
            DownloadTask newTask = new DownloadTask();
            newTask.url = url.trim();
            newTask.checkSum = null != checkSum ? checkSum.trim() : null;
            newTask.desiredName = null != desiredName ? desiredName.trim() : null;
            newTask.state = DownloadTask.STATE_WAITING;

            // Now add it to the task queue
            synchronized (mTaskQueueManager) {
                if (TaskQueueManager.ERROR_OVERSTEP_MAX_LENGTH == mTaskQueueManager.add2WaitingQueue(newTask, jumpQueue)) {
                    return -1;
                }
            }
            return 1;
        }

        @Override
        public int queryState(String url) throws RemoteException {
            // Check parameter
            if (TextUtils.isEmpty(url)) {
                throw new IllegalArgumentException("url should not be null or empty!");
            }

            // Query
            synchronized (mTaskQueueManager) {
                DownloadTask task = mTaskQueueManager.queryAll(url.trim());
                return null == task ? DownloadTask.STATE_NOT_FOUND : task.state;
            }
        }

        @Override
        public int cancelOne(String url) throws RemoteException {
            if (TextUtils.isEmpty(url)) {
                throw new IllegalArgumentException("Failed to cancel the task: " + url + ".");
            }
            synchronized (mTaskQueueManager) {
                DownloadTask task = mTaskQueueManager.queryAll(url);
                if (null == task) {
                    return -1;
                }
                if (task.state == DownloadTask.STATE_WAITING) {
                    return 1;
                }
            }
            return 0;
        }

        @Override
        public void cancelAll() throws RemoteException {
            synchronized (mTaskQueueManager) {
                mTaskQueueManager.clearWaitingQueue();
            }
        }

        @Override
        public void clearOne(String url) throws RemoteException {
            if (TextUtils.isEmpty(url)) {
                String errMsg = "Failed to clear the task: " + url + ".";
                if (BuildConfig.DEBUG) {
                    throw new IllegalArgumentException(errMsg);
                } else {
                    Log.w(Common.COMMON_LOG_TAG, errMsg);
                    return;
                }
            }
            synchronized (mTaskQueueManager) {
                boolean state = true;
                DownloadTask task = mTaskQueueManager.queryInSucceedQueue(url);
                state = null == task ? false : state;
                task = null == task ? mTaskQueueManager.queryInFailedQueue(url) : task;
                if (null != task) {
                    if (state) {
                        mTaskQueueManager.deleteFromSucceedQueue(task);
                    } else {
                        mTaskQueueManager.deleteFromFailedQueue(task);
                    }
                }
            }
        }

        @Override
        public void clearAll() throws RemoteException {
            // TODO
        }

        @Override
        public String getFile(String url) throws RemoteException {
            if (TextUtils.isEmpty(url)) {
                throw new RemoteException("Failed to get the downloaded file: " + url + ".");
            }
            synchronized (mTaskQueueManager) {
                DownloadTask task = mTaskQueueManager.queryInSucceedQueue(url);
                if (null == task) {
                    throw new RemoteException("Task was not found in succeed queue.");
                } else {
                    return task.absolutePath;
                }
            }
        }
    }
}
