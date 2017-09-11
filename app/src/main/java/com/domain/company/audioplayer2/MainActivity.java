package com.domain.company.audioplayer2;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

public class MainActivity extends Activity implements ServiceCallbacks {

    private static final String TAG = "MainActivity";
    private String filePath;
    private Intent intent;
    private PlayerService mService;
    private boolean mBound = false;

    public void back(View view) {

        //toggle(false,true);
        mService.back();
//        seek(-5000);
//        isBacking = true;
    }

    public void fwd(View view) {
        //toggle(false,true);
        mService.forward();
//        if (!isPlaying) {
//            startPlay();
//        }
//        seek(5000);
//        isForward = true;
    }

    public void bback(View view) {
        //toggle(false,true);
        mService.bback();
//        seek(-60000);
//        isBBacking = true;
    }

    public void ffwd(View view) {
        //toggle(false,true);
        mService.ffwd();
//        if (!isPlaying) {
//            startPlay();
//        }
//        seek(60000);
//        isFForward = true;
    }

    public void play(View view) {
        mService.togglePlay();
    }

    public void pause(View view) {
        mService.togglePause();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called " + savedInstanceState);
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
        Log.d(TAG, "binding service... intent=" + intent);
        boolean isBound = bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "binding service end, isBound=" + isBound);
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
            Log.d(TAG, "unbinding...");
            //   unbindService(mConnection);
            Log.d(TAG, "unbinding end");
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
        savedInstanceState.putString("FilePath", filePath);
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
            Log.d(TAG, "stopping existing service");
            stopService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        intent.putExtra("FilePath", filePath);
        Log.d(TAG, "starting service... intent=" + intent);
        startService(intent);
        Log.d(TAG, "starting service end");
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PlayerService.LocalBinder binder = (PlayerService.LocalBinder) service;
            mService = binder.getService();
            mService.setCallbacks(MainActivity.this);
            mBound = true;

            filePath = mService.getFilePath();
            TextView t = (TextView) findViewById(R.id.tvInfo);
            t.setText(filePath);
            mService.play();
            mService.update();

            Log.d(TAG, "service connected");

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    public void info(PlayerInfo pi) {
        final TextView t = (TextView) findViewById(R.id.tvInfo);
        double per = 0.0;
        if (pi.duration != 0) {
            per = 100 * (double) pi.position / (double) pi.duration;
        }
        int curr = pi.position / 1000;
        int total = pi.duration / 1000;
        String name = new File(pi.filePath).getName();
        final String i = String.format("%s %.2f%% %d/%d", name, per, curr, total);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                t.setText(i);

            }
        });
    }

    @Override
    public void seekComplete() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "seekComplete");
                toggle(true, true);
            }
        });
    }

    @Override
    public void stopped() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toggle(false, false);
            }
        });

    }

    @Override
    public void playing() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toggle(true, true);

            }
        });
    }

    @Override
    public void paused() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button b = (Button) findViewById(R.id.back);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.bback);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.fwd);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.ffwd);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.pause);
                b.setEnabled(true);
                b.setText("-->");

                b = (Button) findViewById(R.id.play);
                b.setText("S");
            }
        });
    }

    @Override
    public void unpaused() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button b = (Button) findViewById(R.id.back);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.bback);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.fwd);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.ffwd);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.pause);
                b.setEnabled(true);
                b.setText("||");

                b = (Button) findViewById(R.id.play);
                b.setText("S");
            }
        });
    }

    private void toggle(boolean v, boolean all) {
//        Button b = (Button) findViewById(R.id.back);
//        b.setEnabled(v);
//
//        b = (Button) findViewById(R.id.bback);
//        b.setEnabled(v);
//
//        if (all) {
//            b = (Button) findViewById(R.id.fwd);
//            b.setEnabled(v);
//
//            b = (Button) findViewById(R.id.ffwd);
//            b.setEnabled(v);
//        }
//
//        b = (Button) findViewById(R.id.pause);
//        b.setEnabled(v);
//
//        b = (Button) findViewById(R.id.play);
//        b.setText(v ? "S" : "P");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        Log.d(TAG, "oncreateoptionsmenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.menu2_item1:
//
//
                break;
            default:
                break;
        }

        return true;
    }
}
