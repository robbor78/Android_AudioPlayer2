package com.domain.company.audioplayer2;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private String filePath;
    private Intent intent;
    private PlayerService mService;
    private boolean mBound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called "+savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            filePath = getIntent().getData().getPath();

            TextView t = (TextView) findViewById(R.id.tvInfo);
            t.setText(filePath);

            Log.d(TAG, "onCreate: " + filePath);

            startService(filePath);

        } else {
            Log.d(TAG, "onCreate no intent");

        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        // Bind to LocalService
        Intent intent = new Intent(this, PlayerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("FilePath",filePath);
        Log.d(TAG, "onSaveInstanceState");
    }


    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "received new intent...");
        filePath = intent.getStringExtra("FilePath");
        TextView t = (TextView) findViewById(R.id.tvInfo);
        t.setText(filePath);



    }

    private void startService(String filePath) {
        intent = new Intent(this, PlayerService.class);

        try {
            stopService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        intent.putExtra("FilePath", filePath);
        startService(intent);
        Log.d(TAG, "starting service...");
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PlayerService.LocalBinder binder = (PlayerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            filePath = mService.getFilePath();
            TextView t = (TextView) findViewById(R.id.tvInfo);
            t.setText(filePath);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

}
