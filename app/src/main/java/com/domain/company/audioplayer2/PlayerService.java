package com.domain.company.audioplayer2;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;

public class PlayerService extends IntentService {

    private static final String TAG = "PlayerService";
    private static final int ONGOING_NOTIFICATION_ID = 1;

    private boolean stop = false;
    private boolean isPlaying = false;
    private boolean isPause = false;
    private String filePath;
    private MediaPlayer mediaPlayer = null;
    private ServiceCallbacks serviceCallbacks = null;

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
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        stop = true;
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        stop = false;
        isPlaying = false;
        isPause = false;
        filePath = intent.getStringExtra("FilePath");

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                serviceCallbacks.seekComplete();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mPlayer) {
                stop();
            }
        });

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);//FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra("FilePath", filePath);
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
                //Log.d(TAG, "handling intent... " + filePath);

                info();

                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            // Restore interrupt status.
            Thread.currentThread().interrupt();
        }


        try {
            stop();
            info();
            mediaPlayer.release();
        } finally {
            mediaPlayer = null;
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public String getFilePath() {
        return filePath;
    }

    public void togglePlay() {
        if (isPlaying) {
            stop();
        } else {
            play();
        }
    }

    public void togglePause() {
        if (isPlaying) {
            if (isPause) {
                mediaPlayer.start();
                serviceCallbacks.unpaused();
            } else {
                mediaPlayer.pause();
                serviceCallbacks.paused();
            }
            isPause = !isPause;
        }
    }

    public void stop() {


        if (!isPlaying) {
            return;
        }

        try {
            mediaPlayer.stop();
            mediaPlayer.reset();
//            observer.stop();
//            observer = null;
//            t.setText("ok stop ");
        } catch (Exception e) {
            e.printStackTrace();
//            t.setText("fail stop " + e.getMessage());
        }

        isPlaying = false;
        isPause = false;
        serviceCallbacks.stopped();

    }

    public void play() {

        if (isPlaying) {
            return;
        }

        FileInputStream fis = null;
        try {

            fis = new FileInputStream(filePath);
            mediaPlayer.setDataSource(fis.getFD());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            //mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();


            //observer = new MediaObserver();
            //mediaPlayer.start();
            //new Thread(observer).start();

            isPlaying = true;
            isPause = false;
            serviceCallbacks.playing();


        } catch (IOException e) {
            //t.setText("fail play " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignore) {
                }
            }

        }
    }

    public void back() {
        seek(-5000);
    }

    public void forward() {
        seek(5000);
    }

    public void bback() {
        seek(-60000);
    }

    public void ffwd() {
        seek(60000);
    }

    private void seek(int delta) {
        int np = mediaPlayer.getCurrentPosition() + delta;
        mediaPlayer.seekTo(np);
    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

    private void info() {
        if (serviceCallbacks == null) {
            return;
        }

        PlayerInfo pi = new PlayerInfo();
        pi.filePath = filePath;
        if (mediaPlayer.isPlaying()){
            pi.duration = mediaPlayer.getDuration();
            pi.position = mediaPlayer.getCurrentPosition();
        }

        serviceCallbacks.info(pi);
    }

}
