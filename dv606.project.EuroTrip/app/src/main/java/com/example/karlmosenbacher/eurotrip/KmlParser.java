package com.example.karlmosenbacher.eurotrip;

import android.app.Activity;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Class that downloads and parse a KML file and draws the route on the map.
 * Created by Andree Höög and Karl Mösenbacher on 2015-10-25.
 */
public class KmlParser {
    private final String TAG = "KmlParser";
    private ArrayList<LatLng> polyLine_list;
    private Geocoder geocoder;
    private GoogleMap mMap;
    Context context;

    public KmlParser(Context context, GoogleMap googleMap) {
        this.context = context;
        mMap = googleMap;
        geocoder = new Geocoder(context, Locale.getDefault());  // Locale.US!
    }
    // Start AsyncTask with the chosen route (url)
    public void viewRoute(String s) {
        try {
            URL url = new URL(s);
            AsyncTask task = new RouteRetriever().execute(url);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private class RouteRetriever extends AsyncTask<URL, String, Integer> {
        protected Integer doInBackground(URL... urls) {
            try {
                XmlPullParser receivedKmlData = tryDownloadKmlData(urls[0]);
                int routesFound = tryParsingKmlData(receivedKmlData);
                return routesFound;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private XmlPullParser tryDownloadKmlData(URL url) {
            try {
                XmlPullParser receivedData = XmlPullParserFactory.newInstance().newPullParser();
                receivedData.setInput(url.openStream(),null);
                return receivedData;
            } catch (XmlPullParserException e ) {
                Log.e(TAG, "XmlPullParserException", e);
            } catch (IOException e) {
                Log.e(TAG, "XmlPullParserException", e);
            }
            return null;

        }

        private int tryParsingKmlData(XmlPullParser receivedKmlData) {

            if(receivedKmlData != null) {
                try {
                    return processReceivedData(receivedKmlData);
                } catch (XmlPullParserException e) {
                    Log.e(TAG, "Pull parser failure", e);
                } catch (IOException e) {
                    Log.e(TAG, "IO Exception parsing KML");
                }
            }

            return 0;
        }

        // Process received data and extract all coordinates
        private int processReceivedData(XmlPullParser kmlData)
                throws XmlPullParserException, IOException {
            int routesFound = 0;

            // Find values in KML Route
            String coords = "";

            int eventType = -1;

            // limit to just extract all intermidate coords.
            while (eventType != XmlResourceParser.END_DOCUMENT && routesFound < 1) {
                String tagName = kmlData.getName();

                switch (eventType) {

                    case XmlResourceParser.START_TAG:
                        if (tagName.equals("coordinates")) {
                            coords = "";
                        }
                        break;

                    case XmlResourceParser.TEXT:
                        coords += kmlData.getText();
                        break;

                    case XmlResourceParser.END_TAG:
                        if (tagName.equals("coordinates")) {
                            publishProgress(coords);
                            routesFound++;
                            Log.i(TAG, "routesFound: " + routesFound);
                        }
                        break;
                }
                eventType = kmlData.next();
            }

            if (routesFound == 0) {
                publishProgress();
            }
            return routesFound;
        }


        // Split and process the big string of coordinates and call drawRoute().
        @Override
        protected void onProgressUpdate(String... values) {

            if (values.length == 0) {
                Log.i(TAG, "No data downloaded");
            }
            if (values.length == 1) {
                polyLine_list = new ArrayList<LatLng>();

                // read all coordinates as one big string
                String coords = values[0];

                // split and put into an array where there is white space.
                // each position will have a string on format (long, lat, alt)
                String[] location = coords.split(" ");

                Log.i(TAG, "Length of array: " + location.length);

                // loop through the array, on every position: split where there is a "," sign
                // again to extract and parse each variable.
                for (int i = 0; i < location.length; i++ ) {

                    String[] coordinates = location[i].split(",");
                    double latitude = Double.parseDouble(coordinates[1]);
                    double longitude = Double.parseDouble(coordinates[0]);
                    LatLng pos = new LatLng(latitude, longitude);
                    polyLine_list.add(pos); // add every pos to a LatLng-list

                }
                drawRoute(); // draw route
            }
            super.onProgressUpdate(values);
        }

    }

    // Draw route on map
    public void drawRoute() {

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                Log.i(TAG, "In onMapLoaded");
                Log.i(TAG, "List Size: " + polyLine_list.size());

                // Get coords for start and end destination
                LatLng startPosition = polyLine_list.get(0);
                LatLng endPosition = polyLine_list.get(polyLine_list.size()-1);

                // Extract city name by using the coordinates of start and end positions.
                String startName = "";
                String endName = "";

                try {
                    List<Address> startAddress = geocoder.getFromLocation(startPosition.latitude, startPosition.longitude, 1);
                    List<Address> endAddress = geocoder.getFromLocation(endPosition.latitude, endPosition.longitude, 1);

                    startName = startAddress.get(0).getLocality();
                    endName = endAddress.get(0).getLocality();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mMap.addPolyline(new PolylineOptions()
                        .width(6)
                        .color(Color.BLUE)
                        .add(polyLine_list.toArray(new LatLng[polyLine_list.size()])));

                // Add marker for start position
                mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_directions_train_red_700_18dp))
                        .anchor(0.5f, 0.5f)
                        .position(startPosition)
                        .title(startName));

                // Add marker for end position
                mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_green_900_18dp))
                        .anchor(0.5f, 0.5f)
                        .position(endPosition)
                        .title(endName));

                // fit all markers into the screen
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (int i = 0; i < polyLine_list.size(); i++) {
                    builder.include(polyLine_list.get(i));
                }

                LatLngBounds bounds = builder.build();
                mMap.getUiSettings().setAllGesturesEnabled(true);
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200));


            }
        });
    }
}
