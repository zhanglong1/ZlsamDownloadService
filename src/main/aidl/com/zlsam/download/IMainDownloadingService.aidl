package com.zlsam.download;

interface IMainDownloadingService {

    /**
     * Add a new downloading request to the queue.
     *
     * @param url The url from which to download a file, should not be null or empty.
     * @param checkSum Used to check the downloaded file, can be null. Note: not implemented at present!
     * @param desiredName The name that you want to use for saving the downloaded file. If you passing null or empty,
     * or there's already an existed file, the desiredName will not be used.
     * @param jumpQueue If passing true, the task will be added to the first position and will be processed next turn.
     *
     * @return -1 overstep max length 1 succeed
     */
    int add2Queue(String url, String checkSum, String desiredName, boolean jumpQueue);

    /**
     * Query the state of an on-going downloading task using its url.
     *
     * @param url The url of a downloading task, should not be null or empty.
     * @return The state of the task: -1 task not found, 0 waiting in the queue, 1 downloading, 2 downloaded, 3 failed.
     */
    int queryState(String url);

    /**
     * Cancel one downloading task using its task id.
     *
     * @param url The url of a task, should not be null or empty.
     * @return -1 task not found, 0 failed, 1 succeed.
     */
    int cancelOne(String url);

    /**
     * Cancel all on-going tasks.
     */
    void cancelAll();

    void clearOne(String url);

    /**
     *
     */
    void clearAll();

    /**
     * Get the downloaded file.
     * @param url The url of a task.
     * @return The absolute path of the downloaded file if the file has been already downloaded and
     * still exists, or null will be returned.
     */
    String getFile(String url);
}