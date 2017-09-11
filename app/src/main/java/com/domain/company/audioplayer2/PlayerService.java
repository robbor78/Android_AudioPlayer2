package com.domain.company.audioplayer2;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
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
    private MediaPlayer mediaPlayer = new MediaPlayer();
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
        Toast.makeText(this, "PlayerService player starting", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "PlayerService player starting... " + this);
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

        Log.d(TAG, "PlayerService onHandleIntent " + intent + " " + this);

        String action = (String) intent.getExtras().get("DO");
        if (action == null) {

            Log.d(TAG, "PlayerService no intent / default");


            stop = false;
            isPlaying = false;
            isPause = false;
            filePath = intent.getStringExtra("FilePath");

            Log.d(TAG, "creating mp");

            mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    Log.d(TAG, "seek complete");
                    serviceCallbacks.seekComplete();
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mPlayer) {
                    Log.d(TAG, "play complete");
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
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setOngoing(true)
                            .build();

            RemoteViews rv = new RemoteViews(getBaseContext().getPackageName(), R.layout.notification_layout);
            setListeners(rv);

            notification.contentView = rv;


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
        } else if (action.equals("play")) {
            Log.d(TAG, "intent play");
        } else if (action.equals("back")) {
            Log.d(TAG, "intent back");
        } else if (action.equals("fwd")) {
            Log.d(TAG, "intent fwd");

        }
    }


    private void setListeners(RemoteViews rv) {
        Context ctx = getBaseContext();

        Intent playPause = new Intent(ctx, PlayerService.class);
        playPause.putExtra("DO", "playPause");
        playPause.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pPlayPause = PendingIntent.getActivity(ctx, 0, playPause, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.play, pPlayPause);

        Intent back = new Intent(ctx, PlayerService.class);
        back.putExtra("DO", "back");
        back.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pBack = PendingIntent.getActivity(ctx, 0, back, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.back, pBack);

        Intent fwd = new Intent(ctx, PlayerService.class);
        fwd.putExtra("DO", "fwd");
        fwd.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pFwd = PendingIntent.getActivity(ctx, 0, fwd, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.fwd, pFwd);

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

    private void playOrUnpause() {
        if (isPlaying) {
            if (isPause) {
                togglePause();
            }
        } else {
            togglePlay();
        }
    }

    private void unpause() {
        if (isPlaying) {
            if (isPause) {
                togglePause();
            }
        }
    }

    public void stop() {


        if (!isPlaying) {
            return;
        }

        try {
            mediaPlayer.stop();
            mediaPlayer.reset();
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
            Log.d(TAG, "already playing ... nothing to do ... returining " + this);
            return;
        }


        FileInputStream fis = null;
        try {
            Log.d(TAG, "init playing ... " + this);

            fis = new FileInputStream(filePath);
            mediaPlayer.setDataSource(fis.getFD());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mediaPlayer.prepare();
            mediaPlayer.start();

            isPlaying = true;
            isPause = false;
            serviceCallbacks.playing();

            Log.d(TAG, "init playing end");

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

    public void update() {
        if (isPlaying) {
            if (isPause) {
                serviceCallbacks.paused();
            } else {
                serviceCallbacks.playing();
            }
        } else {
            serviceCallbacks.stopped();
        }
    }

    public void back() {
        unpause();
        seek(-5000);
    }

    public void forward() {
        playOrUnpause();
        seek(5000);
    }

    public void bback() {
        unpause();
        seek(-60000);
    }

    public void ffwd() {
        playOrUnpause();
        seek(60000);
    }

    private void seek(int delta) {
        Log.d(TAG, "PlayerService seeking...");
        int np = mediaPlayer.getCurrentPosition() + delta;
        mediaPlayer.seekTo(np);
        Log.d(TAG, "PlayerService seeking end");
    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

    private void info() {
        if (serviceCallbacks == null) {
            return;
        }

        if (isPause) {
            return;
        }

        PlayerInfo pi = new PlayerInfo();
        pi.filePath = filePath;
        if (mediaPlayer.isPlaying()) {
            pi.duration = mediaPlayer.getDuration();
            pi.position = mediaPlayer.getCurrentPosition();
        }

        serviceCallbacks.info(pi);
    }

}
