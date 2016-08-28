package com.example.karlmosenbacher.eurotrip;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

/**
 * A service that handles the countdown timer and notification.
 * Created by Andree Höög and Karl Mösenbacher on 2015-10-21.
 */
public class TimerService extends Service {
    private static final String TAG = "BroadcastService";
    public static final String TIMER_BR = "com.example.karlmosenbacher.eurotrip.timer_br";
    Intent broadcastIntent = new Intent(TIMER_BR);
    CountDownTimer countDownTimer = null;
    static final int startTime = 900000;
    private static final int NOTIFY_ID = 1;
    private TimerService service;

    @Override
    public void onCreate() {
        service = this;
        super.onCreate();
        Thread thread = new Thread(null, work, "Countdown timer");
        thread.start();
    }

    Runnable work = new Runnable() {
        @Override
        public void run() {
            Looper.prepare();
            // start a countdown timer, counting down from 15 minutes.
            countDownTimer = new CountDownTimer(startTime, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    broadcastIntent.putExtra("countdown", millisUntilFinished);
                    sendBroadcast(broadcastIntent);

                    long min = (millisUntilFinished / 1000) / 60; // convert millis to minutes
                    long sec = (millisUntilFinished / 1000) % 60; // convert millis to seconds

                    // Notification that shows remaining time
                    Notification.Builder builder = new Notification.Builder(getApplicationContext());
                    Intent notIntent = new Intent(getApplicationContext(), GameActivity.class);
                    notIntent.setAction(Intent.ACTION_MAIN);
                    notIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    notIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),
                            0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.setContentIntent(pendingIntent)
                            .setOngoing(true)
                            .setContentTitle(getText(R.string.notTitle))
                            .setSmallIcon(R.drawable.ic_directions_car_black_24dp)
                            .setContentText(Long.toString(min) + ":" + Long.toString(sec));
                    Notification not = builder.build();
                    startForeground(NOTIFY_ID, not);

                }

                @Override
                public void onFinish() {
                    Log.i(TAG, "Timer finished");
                    stopSelf();
                }
            };
            countDownTimer.start();
            Looper.loop();
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        countDownTimer.cancel();
        Log.i(TAG, "Timer Cancelled");
        super.onDestroy();
    }
}
