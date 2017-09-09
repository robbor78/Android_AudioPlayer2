package com.domain.company.audioplayer2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private String filePath;
    private Intent intent;

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

            //filePath = savedInstanceState.getString("FilePath");
            TextView t = (TextView) findViewById(R.id.tvInfo);
            t.setText(filePath);

        }

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
}
