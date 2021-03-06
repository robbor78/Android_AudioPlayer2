package com.domain.company.audioplayer2;

import android.Manifest;
import android.app.ActivityManager;
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
import android.widget.SeekBar;

import java.io.File;

public class MainActivity extends AppCompatActivity implements ServiceCallbacks {

    private static final String TAG = "MainActivity";
    private static final String[] INITIAL_PERMS = {Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int INITIAL_REQUEST = 1339;
    private static final String SAVED_ISSTARTED = "IsStarted";
    public static final String EXTRA_KEY_FILEPATH = "FilePath";

    private String filePath;
    private Intent intent;
    private PlayerService mService;
    private boolean mBound = false;
    private SeekBar sb = null;
    private boolean sbChangedByProgram = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called " + savedInstanceState);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (IsAlreadyRunning(savedInstanceState)) {
            Log.d(TAG, "Already running.");
            bindToLocalService();
        } else if (canReadExternalStorage()) {
            Log.d(TAG, "new setup...");
            setup();
            Log.d(TAG, "new setup end.");
        } else {
            requestPermissions(INITIAL_PERMS, INITIAL_REQUEST);
        }
    }

    private boolean IsAlreadyRunning(Bundle b) {
        return (b != null && !b.isEmpty() && b.getBoolean(SAVED_ISSTARTED)) || IsMyServiceRunning(PlayerService.class);
    }

    private boolean IsMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_exit) {
            exit();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exit() {
        unbind();
        stopService();
    }

    private boolean canReadExternalStorage() {
        return (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP ||
                hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE));
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
        startService();
        bindToLocalService();
    }

    private void startService() {
        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            filePath = getIntent().getData().getPath();
            Log.d(TAG, "onCreate: " + filePath);
            setTitle(filePath);
            startService(filePath);
        } else {
            Log.d(TAG, "onCreate no intent");
        }
    }

    private void bindToLocalService() {
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
        if (!isChangingConfigurations()) {
            exit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(SAVED_ISSTARTED, true);
        Log.d(TAG, "onSaveInstanceState");
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "received new intent...");
        filePath = intent.getStringExtra(EXTRA_KEY_FILEPATH);
        setTitle(filePath);
    }

    private void setTitle(String filePath) {
        Log.d(TAG,"setTitle filepath= "+filePath);
        //if (filePath==null) {return;}
        getSupportActionBar().setTitle(new File(filePath).getName());
    }

    private void startService(String filePath) {
        stopService();
        Intent intent = new Intent(this, PlayerService.class);
        intent.putExtra(EXTRA_KEY_FILEPATH, filePath);
        Log.d(TAG, "starting service... intent=" + intent);
        startService(intent);
        Log.d(TAG, "starting service end");
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.d(TAG, "onServiceConnected...");
            PlayerService.LocalBinder binder = (PlayerService.LocalBinder) service;
            mService = binder.getService();
            mBound = mService != null;
            if (mBound) {
                mService.setCallbacks(MainActivity.this);

                filePath = mService.getFilePath();
                setTitle(filePath);
                wiredUpSeekbar();
                mService.play();
                //mService.update();
                Log.d(TAG, "service connected");
            } else {
                Log.d(TAG, "Service NOT connected!");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private void wiredUpSeekbar() {
        sb = (SeekBar) findViewById(R.id.sb);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (sbChangedByProgram) {
                    return;
                }
                mService.seekRelative(i / 100.0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public void info(PlayerInfo pi) {
        final String title = pi.getTitle();
        final String subTitle = pi.getSubTitle();
        final int pos = pi.getPercent();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getSupportActionBar().setTitle(title);
                getSupportActionBar().setSubtitle(subTitle);
                sbChangedByProgram = true;
                //Log.d(TAG, "pos= " + pos);
                sb.setProgress(pos);
                sbChangedByProgram = false;
            }
        });
    }

    @Override
    public void seekComplete() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "seekComplete");
            }
        });
    }

    //    @Override
    private void stopped() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button b = (Button) findViewById(R.id.act_back);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.act_bback);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.act_fwd);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.act_ffwd);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.act_pause);
                b.setEnabled(true);
                b.setText("||");

                b = (Button) findViewById(R.id.act_play);
                b.setText("P");
            }
        });
    }

    //    @Override
    private void playing() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button b = (Button) findViewById(R.id.act_back);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.act_bback);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.act_fwd);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.act_ffwd);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.act_pause);
                b.setEnabled(true);
                b.setText("||");

                b = (Button) findViewById(R.id.act_play);
                b.setText("S");
            }
        });
    }

    //    @Override
    private void paused() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button b = (Button) findViewById(R.id.act_back);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.act_bback);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.act_fwd);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.act_ffwd);
                b.setEnabled(true);

                b = (Button) findViewById(R.id.act_pause);
                b.setEnabled(true);
                b.setText("-->");

                b = (Button) findViewById(R.id.act_play);
                b.setText("S");
            }
        });
    }


    @Override
    public void sendState(PlayerState state) {
        switch (state) {
            case PAUSED:
                paused();
                break;
            case PLAYING:
                playing();
                break;
            case STOPPED:
                stopped();
                break;
        }
    }



    public void play(View view) {
        mService.togglePlay();
    }

    public void pause(View view) {
        mService.togglePause();
    }

    public void back(View view) {
        mService.back();
    }

    public void fwd(View view) {
        mService.forward();
    }

    public void bback(View view) {
        mService.bback();
    }

    public void ffwd(View view) {
        mService.ffwd();
    }

    public void slower(View view) {mService.slower();}

    public void faster(View view) {mService.faster();}

}
