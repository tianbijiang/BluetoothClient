package com.spectocor.tjiang.bluetoothclient;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    private static ConnectionThread ct = null;
    public static TextView textView;
    public static TextView textView2;
    private static boolean cleaning = false;

    public static void log(String str) {
        Log.d("Client", str);
    }

    public static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case ConnectionThread.MSG_COUNTER: {
                        textView.setText(String.valueOf((short) msg.obj));
                        break;
                    }
                    case ConnectionThread.CL_STATUS: {
                        textView2.setText((boolean) msg.obj ? "Client: connected" : "Client: disconnected");
                        break;
                    }
                    default:
                        break;
                }
            }
        }
    }

    public final MyHandler mHandler = new MyHandler(this);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        log("entering onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        textView2.setText("Client: disconnected");
    }

    @Override
    public void onResume() {
        log("entering onResume()");
        super.onResume();
    }

    @Override
    public void onDestroy() {
        log("entering onDestroy()");
        clean();
        super.onDestroy();
    }

    public void connect(View view) {
        if (ct == null) {
            log("create new ConnThread");
            ct = new ConnectionThread(mHandler);
        } else
            log("ConnThread exists");
        if (!ct.isRunning()) {
            log("connecting to server...");
            ct.setContext(getApplicationContext());
            ct.thread.start();
        } else
            log("ConnThread is running");
    }

    public void disconnect(View view) {
        log("entering disconnect()");
        clean();
    }

    public static void clean() {
        if (!cleaning && ct != null) {
            cleaning = true;
            log("cleaning...");
            if (ct.isRunning()) {
                ConnectionThread.clean();
            }
            ct = null;
            log("CT cleaned");
            cleaning = false;
        }
    }
}