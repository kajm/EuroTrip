package com.example.karlmosenbacher.eurotrip;

import com.google.android.gms.maps.model.LatLng;

/**
 * An object containing all necessary info about a trip. After a trip is downloaded
 * from FireBase a new object will be created from this information.
 * Created by Andree Höög and Karl Mösenbacher on 2015-10-21.
 */
public class Trip {
    private String startcity, endcity, q10P, q8P, q6P, q4P, q2P, kmlUrl, qID, explanation, zoom_latlng;

    public Trip() {

    }

    public String getStartcity() {
        return startcity;
    }

    public String getEndcity() {
        return endcity;
    }

    public String getQ10P() { return q10P; }

    public String getQ8P() {
        return q8P;
    }

    public String getQ6P() {
        return q6P;
    }

    public String getQ4P() {
        return q4P;
    }

    public String getQ2P() {
        return q2P;
    }

    public String getKmlUrl() {
        return kmlUrl;
    }

    public String getqID() {
        return qID;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getZoom_latlng() {
        return zoom_latlng;
    }

    public LatLng getZoom_coords() {
        String zoom_coords = getZoom_latlng();

        String[] coords_arr = zoom_coords.split(", ");
        LatLng coords = new LatLng(Double.parseDouble(coords_arr[0]), Double.parseDouble(coords_arr[1]));

        return coords;
    }
}
