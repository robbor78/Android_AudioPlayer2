package com.domain.company.audioplayer2;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startService();

    }

    private void startService() {
        Intent intent = new Intent(this, PlayerService.class);
        startService(intent);
        Log.d(TAG, "starting service...");
    }
}
