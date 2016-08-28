package com.example.karlmosenbacher.eurotrip;

import android.os.Bundle;
import android.preference.PreferenceActivity;


/**
 * Created by Karl Mösenbacher and Andrée Höög on 2015-10-26.
 */
public class EuroTripPreferenceActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);

    }
}
