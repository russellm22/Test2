package com.example.russe.test;

public class Marker {

    // define public vars here
    public double latti;
    public double longi;
    //public String gridLocation;

    // one constructor method
    public Marker(double lat, double longg) {
        latti = lat;
        longi = longg;
        //gridLocation = gridLoc;

    }

    public double getLatti() {
        return latti;
    }

    public double getLongi() {
        return longi;
    }

}
