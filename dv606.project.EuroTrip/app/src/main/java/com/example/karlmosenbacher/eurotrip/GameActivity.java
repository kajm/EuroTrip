package com.example.karlmosenbacher.eurotrip;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

/**
 * Class that handles gameplay and functionality related to the game.
 * Created by Andrée Höög and Karl Mösenbacher on 18/10-2015
 */
public class GameActivity extends AppCompatActivity {
    private static final String TAG = "BroadcastService - Game";
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Button btn_play, submit, btn_next;
    private TextView textViewTimer, questionView, currentPointView;
    private EditText input;
    private static final int amountToMoveRight = 500, amountToMoveDown = -625;
    private GameActivity gameActivity;
    private Firebase mFirebase;
    private Trip currentTrip;
    private int currentPoints, currentQuestion, totalScore, tripNumber;
    private String[] trips; // Each "round" is five trips;
    private boolean finalRound, isFinished = false, vibrate, playEffects;
    private KmlParser parser;
    private SQLiteDatabase database;
    private SharedPreferences prefs;
    private SharedPreferences.Editor edit;
    private String points = "", scorePerTrip = "", popup = "";
    private Vibrator vibrator;
    private MediaPlayer player;
    private boolean roundFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        getSupportActionBar();
        setUpMapIfNeeded();
        gameActivity = this;
        parser = new KmlParser(gameActivity, mMap);
        setUpFirebase();
        openOrCreateDB();

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        edit = prefs.edit();
        currentPoints = 0;
        tripNumber = 0;
        totalScore = 0; // currently set to 0 but will be loaded from sharedpref.

        textViewTimer = (TextView) findViewById(R.id.timer);
        questionView = (TextView) findViewById(R.id.current_question);
        currentPointView = (TextView) findViewById(R.id.current_points);
        input = (EditText) findViewById(R.id.answer);
        questionView.setMovementMethod(new ScrollingMovementMethod());

        btn_next = (Button) findViewById(R.id.next_trip);
        submit = (Button) findViewById(R.id.submit);
        btn_play = (Button) findViewById(R.id.play);

        initiateOnClickListeners();
    }

    private void openOrCreateDB() {
        database = openOrCreateDatabase("result", MODE_PRIVATE, null);
    }

    private void initiateOnClickListeners() {

        // User submits answer
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                vibrate(50);
                // compare input answer to correct answer
                String answer = input.getText().toString().toUpperCase();
                String correct_answer = currentTrip.getEndcity().toUpperCase();

                if (answer.equals(correct_answer)) {
                    CharSequence charSequence  = getText(R.string.correct_answer) + String.valueOf(currentPoints) + getText(R.string.points) +
                            getText(R.string.explanation) + currentTrip.getExplanation();
                    questionView.setText(charSequence);
                    parser.viewRoute(currentTrip.getKmlUrl()); // download and parse KML file and display the route
                    totalScore += currentPoints;
                    scorePerTrip += currentPoints + ","; // used for saving accumulated points for each trip
                    Log.i(TAG, "ScorePerTrip: " + scorePerTrip);
                    popup = currentTrip.getExplanation();
                    points = getText(R.string.explanation).toString();

                    playSoundEffect(R.raw.correctanswer);

                } else {
                    questionView.setText(R.string.wrong_answer);
                    scorePerTrip += 0 + ",";
                    Log.i(TAG, "ScorePerTrip: " + scorePerTrip);
                    vibrate(750);

                    playSoundEffect(R.raw.wronganswer);
                }
                unregisterReceiverAndStopService();
                Log.i(TAG, "TotalScore= " + totalScore);
                submit.setEnabled(false);
                input.setEnabled(false);

                if (finalRound) {
                    endRound();
                }
                btn_next.setVisibility(View.VISIBLE);

            }
        });

        // User press play button
        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Util.checkInternetConnection(gameActivity)) {
                    new Thread(new Runnable() {
                        public void run() {
                            btn_play.post(new Runnable() {
                                public void run() {
                                    Animation animation = new TranslateAnimation(0, amountToMoveRight, 0, amountToMoveDown);
                                    animation.setDuration(750);
                                    btn_play.startAnimation(animation);
                                    btn_play.setVisibility(View.GONE);
                                    submit.setVisibility(View.VISIBLE);
                                    input.setVisibility(View.VISIBLE);
                                    tripNumber++;
                                    vibrate(50);
                                    roundFinished = false;

                                }
                            });
                            getTripFromFirebase();
                            Log.i(TAG, "Started TimerService");
                        }
                    }).start();
                } else {
                    Toast.makeText(gameActivity, R.string.toastmsg, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // User presses next trip after a trip is finished
        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Util.checkInternetConnection(gameActivity)) {
                    btn_next.setVisibility(View.GONE);
                    btn_next.setText(R.string.next_trip);
                    registerReceiver(broadcastReceiver, new IntentFilter(TimerService.TIMER_BR));
                    getTripFromFirebase();
                    input.setEnabled(true);
                    submit.setEnabled(true);
                    input.setText("");
                    tripNumber++;
                    vibrate(50);
                    setUpMap();
                    roundFinished = false;
                    if (isFinished) {
                        Log.i(TAG, "inside isFinished");
                        isFinished = false;
                    }
                } else {
                    Toast.makeText(gameActivity, R.string.toastmsg , Toast.LENGTH_SHORT).show();
                }

            }
        });

        questionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog dialog = new Dialog(gameActivity);
                dialog.setContentView(R.layout.question_popup);
                dialog.setTitle(points);
                TextView text = (TextView) dialog.findViewById(R.id.popup_text);
                text.setText(popup);
                text.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        });
    }

    //gets a random trip from firebase db.
    private void getTripFromFirebase() {

        mFirebase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                Long nbrOfQuestions = dataSnapshot.child("Questions").getChildrenCount(); // get amount of availible trips
                DataSnapshot postSnapshot = dataSnapshot.child("Questions").child(randomQuestion(nbrOfQuestions)); // randomize and get a specific trip
                currentTrip = postSnapshot.getValue(Trip.class);
                startService(new Intent(gameActivity, TimerService.class));
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    // Unregister receiver and stop service.
    private void unregisterReceiverAndStopService() {
        try {
            unregisterReceiver(broadcastReceiver);
            stopService(new Intent(gameActivity, TimerService.class));
        } catch (Exception e) {
            Log.i(TAG, "Unregister Receiver stop service " + e);
        }
    }

    // Randomizes a question within the range of availible questions.
    private String randomQuestion(long numberOfQuestions) {
        Random rand = new Random();
        currentQuestion = rand.nextInt((int) numberOfQuestions);
        Log.i(TAG, "Slumpat värde: " + currentQuestion);

        int i = 0;
        // Check if new trip already have been traveled. If so, randomize a new one.
        while (i < trips.length) {

            if (String.valueOf(currentQuestion).equals(trips[i])) {
                Log.i(TAG, "Trip already exists, randomize a new one");
                currentQuestion = rand.nextInt((int) numberOfQuestions);
                i = 0;
            } else {
                i++;
            }
        }
        rememberQuestion(currentQuestion);
        return String.valueOf(currentQuestion);
    }

    // Save trips to an array, then saves it into preference so next time we launch
    // the game it will remember which trips we have done so we can't go to same place in a sequence of five trips.
    private void rememberQuestion(int currentQuestion) {

        for (int i = 0; i < trips.length; i++) {
            if (i == trips.length - 1 && trips[i] == null) {
                trips[i] = String.valueOf(currentQuestion);
                Log.i(TAG, "Trips: " + java.util.Arrays.toString(trips));
                Log.i(TAG, "Array is now full, save name and date and total score in database");
                finalRound = true;
                return;
            }
            if (trips[i] == null) {
                trips[i] = String.valueOf(currentQuestion);
                Log.i(TAG, "Trips: " + java.util.Arrays.toString(trips));
                finalRound = false;
                return;
            }
        }
    }

    private void endRound() {
        btn_next.setText(R.string.play_again);
        unregisterReceiverAndStopService();
        Toast.makeText(this, "Game Finished\nYour total score was: " + totalScore, Toast.LENGTH_LONG).show();

        // setup alert dialog allowing user to save his result
        final EditText editText = new EditText(gameActivity);
        new AlertDialog.Builder(this)
                .setTitle(getText(R.string.alert_title).toString() + totalScore)
                .setMessage(R.string.enter_name)
                .setView(editText)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE); // hide keyboard
                        imm.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
                        dialog.cancel();
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE); // hide keyboard
                        imm.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
                        saveToDB(editText.getText().toString(), totalScore);
                        totalScore = 0;
                        scorePerTrip = "";
                    }
                }).create().show();
        trips = new String[5];
        isFinished = true;
        tripNumber = 0;
    }

    // Initiates firebase database and initiate array where current trips in a round will be saved.
    private void setUpFirebase() {
        Firebase.setAndroidContext(gameActivity);
        mFirebase = new Firebase("https://blistering-torch-8544.firebaseio.com");
        trips = new String[5];
        Log.i(TAG, "oncreate string array: " + java.util.Arrays.toString(trips));
    }

    // save results after 5th round in SQL database.
    private void saveToDB(String name, int totalScore) {
        SQLiteDatabase resultdb = openOrCreateDatabase("result", MODE_PRIVATE, null);
        resultdb.execSQL("CREATE TABLE IF NOT EXISTS resultTable (_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "Trip1 INT(2), Trip2 INT(2), Trip3 INT(2), Trip4 INT(2), Trip5 INT(2), " +
                "Name VARCHAR, Date DATE, Totalpoints INT(2));");

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String date = df.format(Calendar.getInstance().getTime()); // get todays date

        String[] temp = scorePerTrip.split(",");

        resultdb.execSQL("INSERT INTO resultTable (Trip1, Trip2, Trip3, Trip4, Trip5, Name, Date, Totalpoints) " +
                "VALUES ('" + temp[0] + "','" + temp[1] + "','" + temp[2] + "','" + temp[3] + "','" + temp[4] + "','" + name + "','" + date + "','" + totalScore + "');");

    }


    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(48.957239, 16.164164), 0));
        mMap.getUiSettings().setAllGesturesEnabled(false);
    }

    // Every second the gameactivity receievs a broadcast with and timercounter update.
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Update gui with new question etc.
//            Log.i(TAG, "Received Broadcast");
            if (intent.getExtras() != null) {
                long millisUntilFinished = intent.getLongExtra("countdown", 0);
                long min = (millisUntilFinished / 1000) / 60; // convert millis to minutes
                long sec = (millisUntilFinished / 1000) % 60; // convert millis to seconds
                updateTimer(min, sec); // update textview
            }
        }
    };

    // This is where the gui will be updated.
    private void updateTimer(long min, long sec) {
        String minutes = String.valueOf(min);
        String seconds = String.valueOf(sec);
        if (sec < 10) {
            seconds = "0" + seconds;
        }
        String timeLeft = minutes + ":" + seconds;
        Log.i(TAG, "min: " + min + " sec: " + sec);

        if (min == 0 && sec == 1){

            points = getText(R.string.time_is_up).toString();
            questionView.setText(R.string.time_is_up_msg);
            currentPoints = 0;
            roundFinished = true;
            timeIsUp();
        }

        if (min < 15 && min >= 12) {

            points = getText(R.string.ten_points).toString();
            questionView.setText(currentTrip.getQ10P());
            popup = currentTrip.getQ10P();
            currentPoints = 10;


        } else if (min < 12 && min >= 9) {

            points = getText(R.string.eight_points).toString();
            questionView.setText(currentTrip.getQ8P());
            popup = currentTrip.getQ8P();
            currentPoints = 8;
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentTrip.getZoom_coords(), 3));

        } else if (min < 9 && min >= 6) {

            points = getText(R.string.six_points).toString();
            questionView.setText(currentTrip.getQ6P());
            popup = currentTrip.getQ6P();
            currentPoints = 6;
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentTrip.getZoom_coords(), 4));

        } else if (min < 6 && min >= 3) {

            points = getText(R.string.four_points).toString();
            questionView.setText(currentTrip.getQ4P());
            popup = currentTrip.getQ4P();
            currentPoints = 4;
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentTrip.getZoom_coords(), 5));

        } else if (min < 3 && min >= 0 && !roundFinished) {

            points = getText(R.string.two_points).toString();
            questionView.setText(currentTrip.getQ2P());
            popup = currentTrip.getQ2P();
            currentPoints = 2;
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentTrip.getZoom_coords(), 8));

        }

        if(roundFinished) {
            textViewTimer.setText("0:00");
        } else {
            textViewTimer.setText(timeLeft);
        }

        currentPointView.setText(points + getText(R.string.trip2) + tripNumber + "/5) ");

    }

    private void timeIsUp() {
        vibrate(750);
        scorePerTrip += 0 + ",";
        unregisterReceiverAndStopService();
        Log.i(TAG, "TotalScore = " + totalScore);
        submit.setEnabled(false);
        input.setEnabled(false);

        if (finalRound) {
            endRound();
        } else {
            btn_next.setVisibility(View.VISIBLE);
        }
    }

    // Make vibration if selected in settings
    public void vibrate(int time) {

        if (vibrate) {
            vibrator.vibrate(time);
        }
    }

    // Play sound effects
    public void playSoundEffect(int song) {

        if (player != null) {
            System.out.println("Musicplayer reset!");
            player.reset();
        }

        if (playEffects) {
            System.out.println("Musicplayer plays!");
            player = MediaPlayer.create(getApplicationContext(), song);
            player.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(broadcastReceiver, new IntentFilter(TimerService.TIMER_BR));
        Log.i(TAG, "Registered broadcast receiver");

        // load trips from prefs
        for (int i = 0; i < trips.length; i++) {
            trips[i] = prefs.getString("Trip_" + i, null);
        }

        // if number of trips has reached 5, create a new one.
        if (trips[4] != null && isFinished) {
            trips = new String[5];
        }
        Log.i(TAG, "Trips in onResume: " + java.util.Arrays.toString(trips));

        tripNumber = prefs.getInt("tripNumber", 0);
        scorePerTrip = prefs.getString("scorePerTrip", "");
        totalScore = prefs.getInt("totalScore", 0);

        setUpMapIfNeeded();

        // Get shared preference for vibrating
        vibrate = prefs.getBoolean("vibrate", false);

        playEffects = prefs.getBoolean("soundeffects", false);
    }

    @Override
    protected void onPause() {
        try {
            unregisterReceiver(broadcastReceiver);
//            Log.i(TAG, "Unregistered broadcast receiver - onPause");
        } catch (Exception e) {
            Log.i(TAG, "onPause " + e);
        }

        // Save trip into shared prefs.
        for (int i = 0; i < trips.length; i++) {
            edit.remove("Trip_" + i); // remove old
            edit.putString("Trip_" + i, trips[i]); // add new questionId
        }
        edit.putInt("tripNumber", tripNumber);
        edit.putString("scorePerTrip", scorePerTrip);
        edit.putInt("totalScore", totalScore);
//        Log.i(TAG, "Inside onPause: ScorePerTrip:" + scorePerTrip);
        edit.commit();

        super.onPause();
    }

    @Override
    protected void onStop() {
        try {
            unregisterReceiver(broadcastReceiver);
//            Log.i(TAG, "Unregistered broadcast receiver - onStop");
        } catch (Exception e) {
            Log.i(TAG, "onStop " + e);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(gameActivity, TimerService.class));
//        Log.i(TAG, "Stopped service");

        for (int i = 0; i < trips.length; i++) {
            edit.remove("Trip_" + i); // remove old
//            Log.i(TAG, "inside forloop in ondestroy - Trip_" + i + " = " + trips[i]);
        }
        edit.remove("tripNumber");
        edit.remove("scorePerTrip");
        edit.remove("totalScore");
        edit.commit();
    }

    /* Shows an AlertDialog if user press the back button. If user presses the back button
     the game will reset.*/
    @Override
    public void onBackPressed() {
        String msg = "";
        if (isFinished) {
            msg = getText(R.string.exit1).toString();
        } else {
            msg = getText(R.string.exit2).toString();
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.exit3)
                .setMessage(msg)
                .setNegativeButton("NO", null)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        edit.commit();
                        finish();
                    }
                }).create().show();
    }
}
