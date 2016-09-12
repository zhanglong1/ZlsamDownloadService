package com.zlsam.download;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TestActivity extends Activity {

    private Button mAddTaskBtn;
    private Button mAddTakJumpBtn;
    private Button mDeleteTaskBtn;
    private TextView mLogTxt;
    private Button mCheckBtn;
    private EditText mTaskIndexEdit;
    private ScrollView mLogScroller;
    private List<String> mFinishedTasks = new ArrayList<>();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initViews();
    }

    private DownloadBroadcasrReceiver mReceiver;

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MainDownloadingService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // receiver
        mReceiver = new DownloadBroadcasrReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.zlsam.download.action.DOWNLOAD_SUCCEED");
        filter.addAction("com.zlsam.download.action.DOWNLOAD_FAILED");
        filter.addAction("com.zlsam.download.action.CLEAR_ONE");
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
        unbindService(mConnection);
    }

    private void appendLog(String log) {
        mLogTxt.append(log + "\n\n");
        mLogScroller.smoothScrollBy(0, mLogScroller.getHeight());
    }

    private void initViews() {
        // Instances
        mAddTaskBtn = (Button) findViewById(R.id.btn_add_task);
        mAddTakJumpBtn = (Button) findViewById(R.id.btn_add_task_jump);
        mDeleteTaskBtn = (Button) findViewById(R.id.btn_delete_task);
        mLogTxt = (TextView) findViewById(R.id.txt_log);
        mCheckBtn = (Button) findViewById(R.id.btn_check_state);
        mTaskIndexEdit = (EditText) findViewById(R.id.edit_task_index);
        mLogScroller = (ScrollView) findViewById(R.id.scroll_log);

        mAddTaskBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUrlIndex++ < mUrls.length) {
                    String url = mUrls[mUrlIndex];
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
                }
            }
        });

        mAddTakJumpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUrlIndex++ < mUrls.length) {
                    String url = mUrls[mUrlIndex];
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
                }
            }
        });

        mDeleteTaskBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized (mFinishedTasks) {
                    if (mFinishedTasks.size() <= 0) {
                        appendLog("Clear a finished task: no finished task.");
                    } else {
                        try {
                            mDownloadService.clearOne(mFinishedTasks.get(mFinishedTasks.size() - 1));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        mCheckBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = 0;
                try {
                    index = Integer.parseInt(mTaskIndexEdit.getText().toString());
                } catch (Exception e) {
                    return;
                }
                if (index < mUrls.length) {
                    int state = 0;
                    String url = mUrls[index];
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
                }
            }
        });
    }

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

    private int mUrlIndex = 0;
    private String[] mUrls = {
            "http://resource.yeah-info.com/yeahgms/2ea2994f-2c0e-44a7-8067-ffc4d6789519.apk",
            "http://resource.yeah-info.com/yeahgms/22077f6a-2222-4d66-9819-37bfe0b94899.apk",
            "http://resource.yeah-info.com/yeahgms/5f4411bb-eeef-4f1c-aa8e-620134dd159d.apk",
            "http://resource.yeah-info.com/yeahgms/bfbc97ef-87e8-4d3d-af06-56e0fbe46d44.apk",
            "http://resource.yeah-info.com/yeahgms/b7e291b5-0780-48d9-bd8b-e9b6b9ce95d3.apk",
            "http://resource.yeah-info.com/yeahgms/3824e602-6445-443d-8d2e-4ca865490376.apk",
            "http://resource.yeah-info.com/yeahgms/8164103c-1efe-4dab-b4fe-de5898b1c5ce.apk",
            "http://resource.yeah-info.com/yeahgms/ab8c571a-f398-4bb9-993e-86755a47b0d6.apk",
            "http://resource.yeah-info.com/yeahgms/6168f79c-3dfc-43ac-988c-60e79b912f7c.apk",
            "http://resource.yeah-info.com/yeahgms/33c6d6e8-e71a-4abc-a43c-1b26610c2964.apk",
            "http://resource.yeah-info.com/yeahgms/dfa5f671-03a0-47e0-9f9a-cbad97f18b7d.apk",
            "http://resource.yeah-info.com/yeahgms/9fadfa50-eeaa-4a5f-ba1c-51664a0868d3.apk",
            "http://resource.yeah-info.com/yeahgms/350c2a77-ea11-4413-9a67-57a2bcd3de0b.apk",
            "http://resource.yeah-info.com/yeahgms/2fd218c1-f67e-4977-9940-4ee13b41f942.apk",
            "http://resource.yeah-info.com/yeahgms/6bbdb55b-b563-4f81-b3d0-294832b9d71d.apk",
            "http://resource.yeah-info.com/yeahgms/7a28b7ef-ae58-49ca-90f7-74e3726361c0.apk",
            "http://resource.yeah-info.com/yeahgms/4188b72f-ecc2-4224-b3a3-8d6266d853ec.apk",
            "http://resource.yeah-info.com/yeahgms/caedb025-ca09-4bdc-a27d-ba946ed9de98.apk",
            "http://resource.yeah-info.com/yeahgms/7e30950e-a633-4f0d-8204-9cc80ca4d9fc.apk",
            "http://resource.yeah-info.com/yeahgms/7f813f56-bce8-4e07-a9d6-eb6ca27fe97e.apk",
            "http://resource.yeah-info.com/yeahgms/669619ac-ff68-4c2d-8a96-d1665e4be824.apk",
            "http://resource.yeah-info.com/yeahgms/f430d680-67b0-48e4-a817-fe75d0038450.apk",
            "http://resource.yeah-info.com/yeahgms/e3a952a5-c2aa-4fb2-ba33-f8bd56be3453.apk",
            "http://resource.yeah-info.com/yeahgms/56bd2f57-c8b1-4715-9aa9-afed67d7e44f.apk",
            "http://resource.yeah-info.com/yeahgms/a43fb253-f679-4972-adce-e6814a2d2294.apk",
            "http://resource.yeah-info.com/yeahgms/e25c3a20-d885-4832-921b-e727d1b50965.apk",
            "http://resource.yeah-info.com/yeahgms/7d51919a-bcc6-440a-a811-1c2d509fd504.apk",
            "http://resource.yeah-info.com/yeahgms/afb7c8b4-bd42-4229-8fce-754b06c5d4f7.apk",
            "http://resource.yeah-info.com/yeahgms/77f673c5-639a-47e4-a971-d6e5acc9700f.apk",
            "http://resource.yeah-info.com/yeahgms/b8135090-b2cd-4521-b395-589a14026918.apk",
            "http://resource.yeah-info.com/yeahgms/c06ea0de-3833-4ecc-823a-6ee43110316a.apk",
            "http://resource.yeah-info.com/yeahgms/32b3a72d-101b-47b9-a51e-2a4b4926c6bd.apk",
            "http://resource.yeah-info.com/yeahgms/b0338adb-bacc-47d7-bc89-5c3ea711bce7.apk",
            "http://resource.yeah-info.com/yeahgms/a5f4cea7-2719-4975-9428-eebd60decf03.apk",
            "http://resource.yeah-info.com/yeahgms/995b92c1-8f81-4042-8b12-a2c548d6529f.apk",
            "http://resource.yeah-info.com/yeahgms/8a505e7c-dc24-4ce3-a06d-ee2b34db40c9.apk",
            "http://resource.yeah-info.com/yeahgms/e3ab40ce-baca-4037-a33f-f6b29ff52b84.apk",
            "http://resource.yeah-info.com/yeahgms/197086ab-05ce-44d0-b4f9-aa17870c1eef.apk",
            "http://resource.yeah-info.com/yeahgms/ef79d0ae-4d78-42d3-bad7-981575fcd313.apk",
            "http://resource.yeah-info.com/yeahgms/447d7ae4-affc-422d-9b71-3d695991a551.apk",
            "http://resource.yeah-info.com/yeahgms/0a53a064-9f13-4c10-9587-3bee83aca74d.apk",
            "http://resource.yeah-info.com/yeahgms/5734eb95-f54a-436f-99ac-a5fdf61e6e56.apk",
            "http://resource.yeah-info.com/yeahgms/7691b615-4d25-4b96-9257-ef5a19698c1d.apk",
            "http://resource.yeah-info.com/yeahgms/324c89fc-4c77-407b-bf75-59eb2da7b00e.apk",
            "http://resource.yeah-info.com/yeahgms/1e2055c7-03ae-49fe-86f6-160a8f69084e.apk",
            "http://resource.yeah-info.com/yeahgms/4f787748-c4d8-4505-8b8e-acfdd026d969.apk",
            "http://resource.yeah-info.com/yeahgms/6d05f5d4-10d7-48d6-b6f9-93f815b0f37f.apk"
    };

    private class DownloadBroadcasrReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case "com.zlsam.download.action.DOWNLOAD_SUCCEED":
                    appendLog("Download a task succeed, url: " + intent.getStringExtra("url") + ", absolutePath: " +
                        intent.getStringExtra("absolutePath"));
                    synchronized (mFinishedTasks) {
                        mFinishedTasks.add(intent.getStringExtra("url"));
                    }
                    break;
                case "com.zlsam.download.action.DOWNLOAD_FAILED":
                    appendLog("Download a task failed, url: " + intent.getStringExtra("url"));
                    synchronized (mFinishedTasks) {
                        mFinishedTasks.add(intent.getStringExtra("url"));
                    }
                    break;
                case "com.zlsam.download.action.CLEAR_ONE":
                    appendLog("Cleared a finished task, url: " + intent.getStringExtra("url"));
                    synchronized (mFinishedTasks) {
                        mFinishedTasks.remove(intent.getStringExtra("url"));
                    }
                    break;
                default:
                    appendLog("Unknown broadcast received.");
            }
        }
    }
}
