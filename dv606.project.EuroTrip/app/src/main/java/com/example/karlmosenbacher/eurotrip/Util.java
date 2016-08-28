package com.example.karlmosenbacher.eurotrip;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Helper class.
 * Created by Andree Höög and Karl Mösenbacher on 2015-10-31.
 */
public class Util {

    // Check if device is connected to internet
    public static boolean checkInternetConnection(Context context) {

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean connectedToInternet = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (connectedToInternet) {
            return true;
        }
        else {
            return false;
        }
    }

}
