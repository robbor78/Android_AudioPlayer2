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

    private boolean stopServiceLoop = false;
    //private boolean isPlaying = false;
    //private boolean isPause = false;
    private PlayerState state = PlayerState.STOPPED;
    private float speed = 1.0f;
    private String filePath;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private ServiceCallbacks serviceCallbacks = null;
    private NotificationManager mNotificationManager = null;
    private Notification.Builder mBuilder = null;
    private RemoteViews rv = null;
    private final IBinder mBinder = new PlayerService.LocalBinder();


    public class LocalBinder extends Binder {
        PlayerService getService() {
            return PlayerService.this;
        }
    }

    public PlayerService() {
        super("PlayerService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "PlayerService player starting... " + intent + " " + this);
        String action = (String) intent.getExtras().get("DO");
        Log.d(TAG, "action= " + action);
        if (action != null) {
            handleIntent(intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "PlayerService onHandleIntent " + intent + " " + this);
        handleIntent(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Player Service onDestroy");
        super.onDestroy();
        stopServiceLoop = true;
    }

    private void handleIntent(Intent intent) {
        String action = (String) intent.getExtras().get("DO");
        Log.d(TAG, "action= " + action);
        if (action == null) {
            Log.d(TAG, "PlayerService no intent / default");
            handleStartup(intent);
        } else if (action.equals("playPause")) {
            Log.d(TAG, "intent play");
            playOrTogglePause();
        } else if (action.equals("back")) {
            Log.d(TAG, "intent back");
            back();
        } else if (action.equals("fwd")) {
            Log.d(TAG, "intent fwd");
            forward();
        }
    }

    private void handleStartup(Intent intent) {
        stopServiceLoop = false;
        state = PlayerState.STOPPED;
        //isPlaying = false;
        //isPause = false;
        filePath = intent.getStringExtra(MainActivity.EXTRA_KEY_FILEPATH);
        wireUpPlayer();
        setupNotification();
        loop();
    }

    private void wireUpPlayer() {
        Log.d(TAG, "creating mp...");
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
        Log.d(TAG, "creating mp end.");
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

        Notification notification = mBuilder.setSmallIcon(R.drawable.notification_icon2)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setContent(rv)
                .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    private void loop() {
        try {
            while (!stopServiceLoop) {
                sendState();
                updateInfo();
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            // Restore interrupt status.
            Thread.currentThread().interrupt();
        }

        try {
            stop();
            //updateInfo();
            mediaPlayer.release();
        } finally {
            mediaPlayer = null;
        }
    }

    private void sendState() {
        if (serviceCallbacks == null) {
            return;
        }
        serviceCallbacks.sendState(state);
    }

    private void setListeners(RemoteViews rv) {
        Context ctx = getBaseContext();

        Intent playPause = new Intent(ctx, PlayerService.class);
        playPause.putExtra("DO", "playPause");
        playPause.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pPlayPause = PendingIntent.getService(ctx, NOT_REQ_PLAY, playPause, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.not_play, pPlayPause);

        Intent back = new Intent(ctx, PlayerService.class);
        back.putExtra("DO", "back");
        back.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pBack = PendingIntent.getService(ctx, NOT_REQ_BACK, back, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.not_back, pBack);

        Intent fwd = new Intent(ctx, PlayerService.class);
        fwd.putExtra("DO", "fwd");
        fwd.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pFwd = PendingIntent.getService(ctx, NOT_REQ_FWD, fwd, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.not_fwd, pFwd);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }

    public String getFilePath() {
        return filePath;
    }

    public void togglePlay() {
        if (state == PlayerState.PLAYING || state == PlayerState.PAUSED) {
            stop();
        } else if (state == PlayerState.STOPPED) {
            play();
        }
    }

    public void togglePause() {
        if (state == PlayerState.PAUSED) {
//        if (isPlaying) {
//            if (isPause) {
            resumePlay();
        } else if (state == PlayerState.PLAYING) {
            pause();
        }
        //isPause = !isPause;
        //state = PlayerState.PLAYING;
        //}
    }

//    public void update() {
//        switch (state) {
//            case PAUSED:
//                serviceCallbacks.paused();
//                break;
//            case STOPPED:
//                serviceCallbacks.stopped();
//                break;
//            case PLAYING:
//                serviceCallbacks.playing();
//                break;
//        }
//    }

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

    public void slower() {
        updateSpeed(-0.25f);
    }

    public void faster() {
        updateSpeed(0.25f);
    }

    private void updateSpeed(float inc) {
        if (state != PlayerState.STOPPED) {
            speed += inc;
            mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(speed));
        }
    }

    public void seekRelative(double i) {
        playOrUnpause();
        Log.d(TAG, "i= " + i);
        seekAbs((int) (mediaPlayer.getDuration() * i));
    }


    public void play() {
        if (state == PlayerState.PLAYING) {
            //if (isPlaying) {
            Log.d(TAG, "already playing ... nothing to do ... returining " + this);
            return;
        }
        if (filePath == null) {
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

            //isPlaying = true;
            //isPause = false;
            state = PlayerState.PLAYING;

            Log.d(TAG, "init playing end");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
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

    private void stop() {
        if (state == PlayerState.STOPPED) {
            //if (!isPlaying) {
            return;
        }

        try {
            mediaPlayer.stop();
            mediaPlayer.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }
        state = PlayerState.STOPPED;
        //isPlaying = false;
        //isPause = false;
    }

    private void playOrTogglePause() {
        if (state == PlayerState.STOPPED) {
            //if (!isPlaying) {
            play();
        } else {
            togglePause();
        }
    }

    private void playOrUnpause() {
        if (state == PlayerState.PAUSED) {
//        if (isPlaying) {
//            if (isPause) {
            togglePause();
            //}
        } else if (state == PlayerState.STOPPED) {
            togglePlay();
        }
    }

    private void unpause() {
        if (state == PlayerState.PAUSED) {
//        if (isPlaying) {
//            if (isPause) {
            togglePause();
//            }
        }
    }

    private void resumePlay() {
        mediaPlayer.start();
        state = PlayerState.PLAYING;
    }

    private void pause() {
        mediaPlayer.pause();
        state = PlayerState.PAUSED;
    }

    private void seek(int delta) {
        Log.d(TAG, "PlayerService seeking, delta= " + delta + "...");
        int np = mediaPlayer.getCurrentPosition() + delta;
        if (np >= mediaPlayer.getDuration()) {
            np = mediaPlayer.getDuration() - 1;
            pause();
            mediaPlayer.seekTo(np);
        } else {
            mediaPlayer.seekTo(np);
        }

        Log.d(TAG, "PlayerService seeking end");
    }

    private void seekAbs(int pos) {
        Log.d(TAG, "PlayerService seeking, pos= " + pos + "...");
        mediaPlayer.seekTo(pos);
        Log.d(TAG, "PlayerService seeking end");
    }

    private void updateInfo() {
        if (serviceCallbacks == null) {
            return;
        }
        if (state != PlayerState.PAUSED) {
            //if (!isPause) {
            PlayerInfo pi = getPlayerInfo();
            updateNotification(pi);
            serviceCallbacks.info(pi);
        }
    }

    private PlayerInfo getPlayerInfo() {
        PlayerInfo pi = new PlayerInfo();
        pi.filePath = filePath;
        if (mediaPlayer.isPlaying()) {
            pi.duration = mediaPlayer.getDuration();
            pi.position = mediaPlayer.getCurrentPosition();
        }
        return pi;
    }

    private void updateNotification() {
        updateNotification(getPlayerInfo());
    }

    private void updateNotification(PlayerInfo pi) {
//        if (isPlaying) {
//            rv.setString(R.id.not_play, "setText", "||" );
//        } else if (isPause) {
//            rv.setString(R.id.not_play, "setText", "-->");
//        } else if (!isPlaying) {
//            rv.setString(R.id.not_play, "setText", "P");
//        }
        rv.setTextViewText(R.id.not_title, pi.getCombined());
        mNotificationManager.notify(ONGOING_NOTIFICATION_ID, mBuilder.build());
    }

}
