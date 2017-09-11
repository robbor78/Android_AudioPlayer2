package com.domain.company.audioplayer2;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
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

import java.io.FileInputStream;
import java.io.IOException;

public class PlayerService extends IntentService {

    private static final String TAG = "PlayerService";
    private static final int ONGOING_NOTIFICATION_ID = 1;
    private static final int NOT_REQ_ACT = 20;
    private static final int NOT_REQ_PLAY = 30;
    private static final int NOT_REQ_BACK = 40;
    private static final int NOT_REQ_FWD = 50;

    private boolean stop = false;
    private boolean isPlaying = false;
    private boolean isPause = false;
    private String filePath;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private ServiceCallbacks serviceCallbacks = null;
    private NotificationManager mNotificationManager = null;
    private Notification.Builder mBuilder = null;
    private RemoteViews rv = null;

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
        Log.d(TAG, "PlayerService player starting... " + this);
        String action = (String) intent.getExtras().get("DO");
        Log.d(TAG, "action= " + action);
        if (action != null) {
            if (action.equals("playPause")) {
                Log.d(TAG, "intent play");
                togglePlay();
            } else if (action.equals("back")) {
                Log.d(TAG, "intent back");
                back();
            } else if (action.equals("fwd")) {
                Log.d(TAG, "intent fwd");
                forward();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void handleIntent(Intent intent) {
        String action = (String) intent.getExtras().get("DO");
        Log.d(TAG, "action= " + action);
        if (action == null) {
            Log.d(TAG, "PlayerService no intent / default");
            handleStartup(intent);
        } else if (action.equals("playPause")) {
            Log.d(TAG, "intent play");
        } else if (action.equals("back")) {
            Log.d(TAG, "intent back");
        } else if (action.equals("fwd")) {
            Log.d(TAG, "intent fwd");
        }
    }

    private void handleStartup(Intent intent) {
        stop = false;
        isPlaying = false;
        isPause = false;

        filePath = intent.getStringExtra(MainActivity.EXTRA_KEY_FILEPATH);

        wireUpPlayer();

        setupNotification();

        loop();

    }

    private void wireUpPlayer() {
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
    }

    private void setupNotification() {

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);//FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra("FilePath", filePath);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, NOT_REQ_ACT, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        rv = new RemoteViews(getBaseContext().getPackageName(), R.layout.notification_layout);
        setListeners(rv);

        mBuilder = new Notification.Builder(this);

        Notification notification = mBuilder.setSmallIcon(R.drawable.notification_icon)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setContent(rv)
                .build();


        notification.contentView = rv;

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    private void loop() {
        try {
            while (!stop) {
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
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        Log.d(TAG, "Player Service onDestroy");
        super.onDestroy();
        stop = true;
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "PlayerService onHandleIntent " + intent + " " + this);
        handleIntent(intent);
    }


    private void setListeners(RemoteViews rv) {
        Context ctx = getBaseContext();

        Intent playPause = new Intent(ctx, PlayerService.class);
        playPause.putExtra("DO", "playPause");
        playPause.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pPlayPause = PendingIntent.getService(ctx, NOT_REQ_PLAY, playPause, PendingIntent.FLAG_UPDATE_CURRENT);
        //rv.setOnClickPendingIntent(R.id.not_play, pPlayPause);

        Intent back = new Intent(ctx, PlayerService.class);
        back.putExtra("DO", "back");
        back.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pBack = PendingIntent.getService(ctx, NOT_REQ_BACK, back, PendingIntent.FLAG_UPDATE_CURRENT);
        //rv.setOnClickPendingIntent(R.id.not_back, pBack);

        Intent fwd = new Intent(ctx, PlayerService.class);
        fwd.putExtra("DO", "fwd");
        fwd.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pFwd = PendingIntent.getService(ctx, NOT_REQ_FWD, fwd, PendingIntent.FLAG_UPDATE_CURRENT);
        //rv.setOnClickPendingIntent(R.id.not_fwd, pFwd);

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

        updateNotification(pi);
        serviceCallbacks.info(pi);
    }

    private void updateNotification(PlayerInfo pi) {
        rv.setTextViewText(R.id.not_title, pi.getCombined());
        mNotificationManager.notify(ONGOING_NOTIFICATION_ID, mBuilder.build());

    }

}
