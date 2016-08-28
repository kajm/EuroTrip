package com.example.karlmosenbacher.eurotrip;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * The main activity of the application. Gives user the opportunity to play the game,
 * show highscore and enter settings.
 * Created by Karl Mösenbacher and Andrée Höög on 18/10 - 2015
 */
public class EuroTripMainActivity extends AppCompatActivity {
    EuroTripMainActivity mainActivity;
    private int currentImage = 0, length;
    ImageView imageView;
    private Button start_btn, score_btn, settings_btn;
    private boolean playMusic, vibrate;
    private SharedPreferences prefs;
    MediaPlayer player;

    // Array containging all images for sliding train
    int[] IMAGE_IDS = {R.drawable.pic1, R.drawable.pic2, R.drawable.pic3,
            R.drawable.pic4, R.drawable.pic5, R.drawable.pic6, R.drawable.pic7,
            R.drawable.pic8, R.drawable.pic9, R.drawable.pic10, R.drawable.pic11};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_euro_trip_main);
        mainActivity = this;
        final Handler mHandler = new Handler();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Start game button
        start_btn = (Button) findViewById(R.id.start);
        start_btn.setOnClickListener(new ButtonClick());

        // Score button
        score_btn = (Button) findViewById(R.id.topScore);
        score_btn.setOnClickListener(new ButtonClick());

        // Settings button
        settings_btn = (Button) findViewById(R.id.settings);
        settings_btn.setOnClickListener(new ButtonClick());


        // Create runnable for posting
        final Runnable mUpdateResults = new Runnable() {
            public void run() {

                AnimatedSlideShow();
            }
        };

        // Delay for every picture
        int delay = 100;

        // Repeat every 0,5 second
        int period = 200;

        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {

            public void run() {

                mHandler.post(mUpdateResults);
            }
        }, delay, period);
    }

    private void AnimatedSlideShow() {

        imageView = (ImageView) findViewById(R.id.lower_image);
        imageView.setImageResource(IMAGE_IDS[currentImage % IMAGE_IDS.length]);
        currentImage++;
    }

    // Inner class that handles button clicks
    public class ButtonClick implements View.OnClickListener {

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        @Override
        public void onClick(View v) {

            if (v == start_btn) {
                vibrate();
                startActivity(new Intent(mainActivity, GameActivity.class));
            }

            if (v == score_btn) {
                vibrate();
                startActivity(new Intent(mainActivity, TopScoreActivity.class));
            }

            if (v == settings_btn) {
                vibrate();
                startActivity(new Intent(getApplicationContext(), EuroTripPreferenceActivity.class));
            }
        }

        // Vibrate device if settings is checked to vibrate
        public void vibrate() {

            if (vibrate) {
                vibrator.vibrate(50);
            }
        }
    }
    @Override
    protected void onResume() {


        // Load preferences
        player = MediaPlayer.create(this, R.raw.ontheroad);

        playMusic = prefs.getBoolean("menumusic", false);
        vibrate = prefs.getBoolean("vibrate", false);

        if (playMusic && !player.isPlaying()) {
            player.seekTo(length);
            player.start();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {

        super.onPause();
        player.pause();
        length = player.getCurrentPosition();
    }

}
