package com.domain.company.audioplayer2;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;

public class PlayerService extends IntentService {

    private static final String TAG = "PlayerService";
    private static final int ONGOING_NOTIFICATION_ID = 1;

    private boolean stop = false;
    private String filePath;

    // Binder given to clients
    private final IBinder mBinder = new PlayerService.LocalBinder();


    public class LocalBinder extends Binder {
        PlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PlayerService.this;
        }
    }


    public PlayerService() {
        super("PlayerService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "player starting", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "player starting...");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        Log.d(TAG,"onDestroy");
        super.onDestroy();
        stop = true;
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        stop = false;
        filePath = intent.getStringExtra("FilePath");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);//FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra("FilePath",filePath);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        Notification notification =
                new Notification.Builder(this)
                        .setContentTitle("AP2 Playing")
                        .setContentText(filePath)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.ticker_text))
                        .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);

        try {
            while (!stop) {
                Log.d(TAG, "handling intent... "+filePath);
                Thread.sleep(10000);
            }
        } catch (InterruptedException e) {
            // Restore interrupt status.
            Thread.currentThread().interrupt();
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public String getFilePath() {
        return filePath;
    }

}
