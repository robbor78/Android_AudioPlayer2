package com.domain.company.audioplayer2;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class MainActivity extends AppCompatActivity implements ServiceCallbacks {

    private static final String TAG = "MainActivity";
    private String filePath;
    private Intent intent;
    private PlayerService mService;
    private boolean mBound = false;

    private static final String[] INITIAL_PERMS = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private static final int INITIAL_REQUEST = 1339;

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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        if (canReadExternalStorage()) {
            setup();
        } else {

            requestPermissions(INITIAL_PERMS, INITIAL_REQUEST);
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_exit) {
            unbind();
            stopService();
            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private boolean canReadExternalStorage() {
        return (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE));
    }

    private boolean hasPermission(String perm) {
        return (PackageManager.PERMISSION_GRANTED == checkSelfPermission(perm));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        setup();
    }

    private void setup() {
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            filePath = getIntent().getData().getPath();
            Log.d(TAG, "onCreate: " + filePath);
            setTitle(filePath);
            startService(filePath);
        } else {

            Log.d(TAG, "onCreate no intent");
        }

        // Bind to LocalService
        Intent intent = new Intent(this, PlayerService.class);
        Log.d(TAG, "binding service... intent=" + intent);
        boolean isBound = bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "binding service end, isBound=" + isBound);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");

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

        unbind();
    }

    private void unbind() {
        if (mBound) {
            try {
                Log.d(TAG, "unbinding...");
                unbindService(mConnection);
                mBound = false;
                Log.d(TAG, "unbinding end");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void stopService() {

        try {
            Log.d(TAG, "stopping existing service");
            Intent intent = new Intent(this, PlayerService.class);
            stopService(intent);
            Log.d(TAG, "stopped existing service");
        } catch (Exception e) {
            e.printStackTrace();
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
        setTitle(filePath);
    }

    private void setTitle(String filePath) {
        getSupportActionBar().setTitle(new File(filePath).getName());
    }

    private void startService(String filePath) {
        stopService();
        Intent intent = new Intent(this, PlayerService.class);
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
            setTitle(filePath);
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
        double per = 0.0;
        if (pi.duration != 0) {
            per = 100 * (double) pi.position / (double) pi.duration;
        }
        int curr = pi.position / 1000;
        int total = pi.duration / 1000;
        final String title = new File(pi.filePath).getName();
        final String subTitle = String.format("%.2f%% %d/%d", per, curr, total);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getSupportActionBar().setTitle(title);
                getSupportActionBar().setSubtitle(subTitle);

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
                b.setText("P");
            }
        });

    }

    @Override
    public void playing() {
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


}
