package com.zlsam.download;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import com.google.gson.Gson;
import com.zlsam.download.common.Common;
import com.zlsam.download.common.SmartLog;
import com.zlsam.download.model.DownloadTask;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by zhanglong on 8/29/2016.
 */
public class TaskDispatcher {

    private static TaskDispatcher sInstance;
    private ExecutorService mDispatcherExecutor;

    /** Will be used in {@link DispatcherRunnable}. */
    private ExecutorService mDownloadingExecutor;

    private TaskQueueManager mQueueManager;
    private DispatcherRunnable mDispatcherRunnable;
    private static Context sContext;

    private boolean mShouldStopAllDownloadingThreads = false;

    public static void init(Context context) {
        if (null == context) {
            String errMsg = "Failed to init TaskDispatcher, the passed-in context was null.";
            if (BuildConfig.DEBUG) {
                throw new IllegalArgumentException(errMsg);
            } else {
                SmartLog.e(Common.COMMON_LOG_TAG, errMsg);
            }
        }
        sContext = context;
    }

    public static TaskDispatcher getInstance() {
        if (null == sInstance) {
            sInstance = new TaskDispatcher();
        }
        return sInstance;
    }

    private TaskDispatcher() {
        mDispatcherRunnable = new DispatcherRunnable();
        mDispatcherExecutor = Executors.newSingleThreadExecutor();
        mDownloadingExecutor = Executors.newCachedThreadPool();
        mQueueManager = TaskQueueManager.getInstance();
    }

    public void start() {
        SmartLog.i(Common.COMMON_LOG_TAG, "Starting the task dispatcher.");
        mDispatcherExecutor.execute(mDispatcherRunnable);
        SmartLog.i(Common.COMMON_LOG_TAG, "Start the task dispatcher successfully.");
    }

    public void stop() {
        SmartLog.i(Common.COMMON_LOG_TAG, "Shutting down the task dispatcher.");

        // Shutdown task dispatcher.
        mDispatcherRunnable.setShouldStop(true);
        mDispatcherExecutor.shutdown();
        try {
            if (!mDispatcherExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                mDispatcherExecutor.shutdownNow();
                if (!mDispatcherExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    SmartLog.w(Common.COMMON_LOG_TAG, "Failed to terminate the task dispatcher, time out.");
                }
            }
        } catch (InterruptedException e) {
            mDispatcherExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            SmartLog.w(Common.COMMON_LOG_TAG, "Some errors happened during terminating the task dispatcher, interrupted exception.");
        }

        // Shutdown downloading thread pool
        mShouldStopAllDownloadingThreads = true;
        mDownloadingExecutor.shutdown();
        try {
            if (!mDownloadingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                mDownloadingExecutor.shutdownNow();
                if (!mDownloadingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    SmartLog.w(Common.COMMON_LOG_TAG, "Failed to terminate the download thread pool, time out.");
                }
            }
        } catch (InterruptedException e) {
            mDownloadingExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            SmartLog.w(Common.COMMON_LOG_TAG, "Some errors happened during terminating the download thread pool, interrupted exception.");
        }

        SmartLog.i(Common.COMMON_LOG_TAG, "Shutdown the task dispatcher successfully.");
    }

    private class DispatcherRunnable implements Runnable {

        private boolean mShouldStop = false;

        @Override
        public void run() {
            while (!mShouldStop) {
                try {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    synchronized (mQueueManager) {
                        DownloadTask task = mQueueManager.getNextWaitingTask();
                        if (null == task) {
                            continue;
                        }
                        int result = mQueueManager.add2ProcessingQueue(task);
                        if (result != TaskQueueManager.SUCCEED) {
                            SmartLog.i(Common.COMMON_LOG_TAG, "Failed to process a downloading task, " +
                                    TaskQueueManager.errCode2ErrMsg(result) + ", we'll try it later, url: " + task.url);
                            continue;
                        }
                        mQueueManager.deleteFromWaitingQueue(task);
                        mDownloadingExecutor.execute(new DownloadRunnable(task, null));
                    }
                } catch (Exception e) {
                    SmartLog.e(Common.COMMON_LOG_TAG, "An error happened in the task dispatcher, " + e.getMessage() + ", we'll ignore it and continue.");
                    e.printStackTrace();
                }
            }
        }

        public void setShouldStop(boolean value) {
            mShouldStop = value;
        }
    }

    private class DownloadRunnable implements Runnable {

        private DownloadTask mTask;
        private String mPathWithFileName;

        public DownloadRunnable(DownloadTask task, String pathWithFileName) {
            if (null == task) { // We should always throw exception here
                throw new IllegalArgumentException("Failed to create the DownloadRunnable, task should not be null or empty.");
            }
            mTask = task;
            mPathWithFileName = TextUtils.isEmpty(pathWithFileName) ? Environment.getExternalStorageDirectory().getPath()+"/Zlsam/Download/"
                    : pathWithFileName;
            File cacheDir = new File(mPathWithFileName);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            mPathWithFileName = mPathWithFileName + mTask.url.substring(mTask.url.lastIndexOf("/") + 1, mTask.url.length());
        }

        @Override
        public void run() {
            SmartLog.i(Common.COMMON_LOG_TAG, "About to download a new file: " + new Gson().toJson(mTask));
            File localFile = null;
            try {
                HttpClient client = new DefaultHttpClient();
                client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000);
                client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 120000);
                HttpGet get = new HttpGet(mTask.url);
                HttpResponse response = client.execute(get);
                HttpEntity entity = response.getEntity();
                long length = entity.getContentLength();
                InputStream is = entity.getContent();
                FileOutputStream fileOutputStream = null;
                if (is != null) {
                    localFile = new File(mPathWithFileName);
                    fileOutputStream = new FileOutputStream(localFile);
                    byte[] buf = new byte[1024];
                    int ch = -1;
                    while ((ch = is.read(buf)) != -1 && !mShouldStopAllDownloadingThreads) {
                        fileOutputStream.write(buf, 0, ch);
                    }
                    if (ch > 0 && mShouldStopAllDownloadingThreads) {// Shutdown by hand
                        fileOutputStream.flush();
                        fileOutputStream.close();
                        localFile.delete();
                        return;
                    }
                }
                fileOutputStream.flush();
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }

                // Succeed
                synchronized (mQueueManager) {
                    mTask.absolutePath = mPathWithFileName;
                    mQueueManager.add2SucceedQueue(mTask);
                    mQueueManager.deleteFromProcessingQueue(mTask);
                    BroadcastUtil.sendDownloadSucceedBroadcast(sContext, mTask.url, mTask.absolutePath);
                }
                SmartLog.i(Common.COMMON_LOG_TAG, "Downloaded a new file: " + new Gson().toJson(mTask));
            } catch (Exception e) {
                SmartLog.e(Common.COMMON_LOG_TAG, "Failed to download a file, errMsg: " + e.getMessage() + ", task: " + new Gson().toJson(mTask) + ".");
                e.printStackTrace();
                synchronized (mQueueManager) {
                    if (null != localFile && localFile.exists()) {
                        localFile.delete();
                    }
                    mQueueManager.add2FailedQueue(mTask);
                    mQueueManager.deleteFromProcessingQueue(mTask);
                    BroadcastUtil.sendDownloadFailedBroadcast(sContext, mTask.url);
                }
                Thread.currentThread().interrupt();
            }
        }
    }

}
