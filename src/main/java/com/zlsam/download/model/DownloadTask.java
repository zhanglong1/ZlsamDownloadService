package com.zlsam.download.model;

/**
 * Created by zhanglong on 8/25/2016.
 */
public class DownloadTask {

    /** The url from which to download a file, should not be null or empty. */
    public String url;

    /** Used to check the downloaded file, can be null. Note: not implemented at present! */
    public String checkSum;

    /**
     * The name that you want to use for saving the downloaded file. If null or empty,
     * or there's already an existed file, the desiredName will not be used.
     */
    public String desiredName;

    /** Only succeed task has non-null and not empty value. */
    public String absolutePath;

    public DownloadTask clone() {
        DownloadTask newTask = new DownloadTask();
        newTask.url = this.url;
        newTask.checkSum = this.checkSum;
        newTask.desiredName = this.desiredName;
        newTask.absolutePath = this.absolutePath;
        return newTask;
    }

    /**  -1 task not found, 0 waiting in the queue, 1 downloading, 2 downloaded, 3 failed. */
    public int state = STATE_WAITING;

    public static final int STATE_NOT_FOUND = -1;
    public static final int STATE_WAITING = 0;
    public static final int STATE_DOWNLOADING = 1;
    public static final int STATE_DOWNLOADED = 2;
    public static final int STATE_FAILED = 3;
}
