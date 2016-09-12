#ZlsamDownloadService

ZlsamDownloadService is an android service that can manage multiple downloading tasks. You can use it
as an alternative of android's standard DownloadManager. It has following features:  

1. thread-safe  
2. support ethernet
3. can be shared with multiple apps
4. persist state
5. multiple tasks
6. support jumping queue

Max waiting task queue size: 20;
Max processing task queue size: 3;
Max succeed task queue size: 20;
Max failed task queue size: 20;

If the parameters above don't meet your requirement, you can changed them in the code. They are defined
in TaskQueueManager.java.

## How to use  

If you want to embed this service in your own project, you can set this project as android library and
reference it from your own project, then you can start and bind the service in your onStart callback:  
 
```
Intent intent = new Intent("com.zlsam.download.DOWNLOAD_SERVICE");  
startService(intent);  
bindService(intent, mConnection, Context.BIND_AUTO_CREATE);  

IMainDownloadingService mDownloadService;
private ServiceConnection mConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mDownloadService  = IMainDownloadingService.Stub.asInterface(service);
        appendLog("Bound to download service.");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mDownloadService = null;
    }
};
```

After you bind the service successfully, you can then call the methods defined in com.zlsam.download.IMainDownloadingService.aidl.

Download:  

```
try {
    int result = mDownloadService.add2Queue(url, null, null, false);
    if (result < 0) {
        appendLog("Add task failed, error code: " + result + ", url: " + url);
    } else {
        appendLog("Add task succeed, url: " + url);
    }
} catch (RemoteException e) {
    e.printStackTrace();
}
```

If you want to jump the queue, you can do it like this:  

```
try {
    int result = mDownloadService.add2Queue(url, null, null, true);
    if (result < 0) {
        appendLog("Add task jump failed, error code: " + result + ", url: " + url);
    } else {
        appendLog("Add task jump succeed, url: " + url);
    }
} catch (RemoteException e) {
    e.printStackTrace();
}
```

Check the status:  

```
try {
    state = mDownloadService.queryState(url);
} catch (RemoteException e) {
    e.printStackTrace();
    appendLog("Check task state: exception, url: " + url);
}
switch (state) {
    case -1:
        appendLog("Check task state: not found, url: " + url);
        break;
    case 0:
        appendLog("Check task state: waiting, url: " + url);
        break;
    case 1:
        appendLog("Check task state: processing, url: " + url);
        break;
    case 2:
        appendLog("Check task state: succeed, url: " + url);
        break;
    case 3:
        appendLog("Check task state: failed, url: " + url);
        break;
}
```

If you want to clear a finished task(include the downloaded file), you can do this:  

```
try {
    mDownloadService.clearOne(mFinishedTasks.get(mFinishedTasks.size() - 1));
} catch (RemoteException e) {
    e.printStackTrace();
}
```

When you leave you page you should unbind the service in your onStop callback:  

```
unbindService(mConnection);
```

## Debug  

You can check the log with this command:  

```
adb logcat ZlsamDownloadService:V *:S
```

## Demo  

You can install and start this project, the TestActivity is a built-in demo.
