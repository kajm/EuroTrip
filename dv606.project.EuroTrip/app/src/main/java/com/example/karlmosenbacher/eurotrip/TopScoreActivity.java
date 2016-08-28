package com.example.karlmosenbacher.eurotrip;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/*
 * Class that handles top score
 * Created by Karl Mösenbacher and Andrée Höög on 24/10-2015
 */
public class TopScoreActivity extends ListActivity {
    private List<String[]> player_list = new ArrayList<>();
    private ListView view;
    private int currentposition;
    private final int SHARE = 0, DETAILED = 1, DELETE = 2;
    private SQLiteDatabase resultdb;
    ListAdapter adapter;
    private Context context;
    SharedPreferences prefs;
    private Boolean vibrate;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.top_score_list);
        context = this;
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        view = getListView();

        // Register for context menu
        registerForContextMenu(view);

        openOrCreateDb();
        readTableValues();


        adapter = new MultiAdapter(this);
        setListAdapter(adapter);


        // Make context menu with listeners
        view.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {

            @Override
            public void onCreateContextMenu(ContextMenu menu, View v,
                                            ContextMenu.ContextMenuInfo menuInfo) {

                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                currentposition = info.position;
                Cursor c = resultdb.rawQuery("SELECT _id FROM resultTable", null);
                c.moveToPosition(currentposition);
                menu.setHeaderTitle(R.string.context_header);
                menu.add(0, SHARE, 0, R.string.share);
                menu.add(0, DETAILED, 0, R.string.show_detailed);
                menu.add(0, DELETE, 0, R.string.delete_score);
                c.close();
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        vibrate = prefs.getBoolean("vibrate", false);

    }


    // Read values from resultTable (small version)
    private void readTableValues() {

        Cursor c = resultdb.rawQuery("SELECT Name, Totalpoints, Date FROM resultTable", null);
        c.moveToFirst();
        while (!c.isAfterLast()) {

            String[] player = {c.getString(0), c.getString(1), c.getString(2)};
            player_list.add(player);
            c.moveToNext();
        }

        c.moveToFirst();
        if (c.getCount() == 0) {
            Toast.makeText(getApplicationContext(), R.string.empty_scoreboard, Toast.LENGTH_SHORT).show();
        }

        // Print RowID for each row in database
        c = resultdb.rawQuery("SELECT _id FROM resultTable", null);
        c.moveToFirst();
        while (!c.isAfterLast()) {
            c.moveToNext();
        }

        c.close();
    }

    // Create table if not exists
    private void openOrCreateDb() {

        resultdb = openOrCreateDatabase("result", MODE_PRIVATE, null);
        resultdb.execSQL("CREATE TABLE IF NOT EXISTS resultTable (_id INTEGER PRIMARY KEY AUTOINCREMENT, Trip1 INT(2), Trip2 INT(2), Trip3 INT(2), Trip4 INT(2), Trip5 INT(2), Name VARCHAR, Date DATE, Totalpoints INT(2));");

    }

    // Delete table
    private void deleteTable() {

        resultdb.delete("resultTable", null, null);
        context.deleteDatabase("result");
    }


    // Adapter for setting up row
    class MultiAdapter extends ArrayAdapter {

        public MultiAdapter(Context context) {
            super(context, R.layout.top_score_row, player_list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row;
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.top_score_row, parent, false);
            } else
                row = convertView;

            String[] summary = player_list.get(position);

            // Set players name on list.
            TextView playerView = (TextView) row.findViewById(R.id.player_name);
            playerView.setText(summary[0]);

            // Set players total score on list.
            TextView scoreView = (TextView) row.findViewById(R.id.total_points);
            scoreView.setText(summary[1]);

            // Set date on list.
            TextView dateView = (TextView) row.findViewById(R.id.date);
            dateView.setText(summary[2]);

            return row;
        }
    }

    public boolean onContextItemSelected(MenuItem item) {


        int menuId = item.getItemId();

        switch (menuId) {
            case SHARE:
                // Extract row information from player_list
                String[] player = player_list.get(currentposition);
                shareResult(player[0], player[1], player[2]);
                vibrate();
                return true;

            case DETAILED:
                viewDetailedScore();
                vibrate();
                return true;

            case DELETE:
                player_list.clear();
                deleteTable();
                ((MultiAdapter) view.getAdapter()).notifyDataSetChanged();
                vibrate();
                return true;

            default:
                return false;
        }
    }

    // Vibrate device if settings is checked to vibrate
    public void vibrate() {

        if (vibrate) {
            vibrator.vibrate(50);
        }
    }

    // Shows a dialog with detailed high score for a specific user.
    private void viewDetailedScore() {


        currentposition = currentposition + 1;
        Cursor c = resultdb.rawQuery("SELECT Trip1, Trip2, Trip3, Trip4, Trip5 FROM resultTable WHERE _id =" + Integer.toString(currentposition), null);
        c.moveToFirst();
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.detailed_score_board);

        Button return_btn = (Button) dialog.findViewById(R.id.return_btn);


        /* Add points and trip number to the detailed score board */
        TextView trip_one_id = (TextView) dialog.findViewById(R.id.trip_id_one);
        trip_one_id.setText("1");
        TextView trip_one_score = (TextView) dialog.findViewById(R.id.trip_one_score);
        trip_one_score.setText(c.getString(0));

        TextView trip_two_id = (TextView) dialog.findViewById(R.id.trip_id_two);
        trip_two_id.setText("2");
        TextView trip_two_score = (TextView) dialog.findViewById(R.id.trip_two_score);
        trip_two_score.setText(c.getString(1));

        TextView trip_three_id = (TextView) dialog.findViewById(R.id.trip_id_three);
        trip_three_id.setText("3");
        TextView trip_three_score = (TextView) dialog.findViewById(R.id.trip_three_score);
        trip_three_score.setText(c.getString(2));

        TextView trip_four_id = (TextView) dialog.findViewById(R.id.trip_id_four);
        trip_four_id.setText("4");
        TextView trip_four_score = (TextView) dialog.findViewById(R.id.trip_four_score);
        trip_four_score.setText(c.getString(3));

        TextView trip_five_id = (TextView) dialog.findViewById(R.id.trip_id_five);
        trip_five_id.setText("5");
        TextView trip_five_score = (TextView) dialog.findViewById(R.id.trip_five_score);
        trip_five_score.setText(c.getString(4));

        return_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("CANCEL DETAILED DIALOG");
                dialog.cancel();
            }
        });
        c.close();
        dialog.show();
    }


    // Lets user choose application to share result
    public void shareResult(String name, String points, String date) {

        // Building String for sharing
        String share = getText(R.string.share_my_result).toString() + name + getText(R.string.share1).toString()
                + points + getText(R.string.share2).toString() + date;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.my_eurotrip)).toString();
        shareIntent.putExtra(Intent.EXTRA_TEXT, share);
        startActivity(Intent.createChooser(shareIntent, getText(R.string.share_choices)));
    }
}
