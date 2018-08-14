package com.example.russe.test;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

@DynamoDBTable(tableName = "motosafe-mobilehub-1051444161-MarkerLocationTest")

public class MarkerLocationTestDO {
    private String _gridLocation;
    private String _markerID;
    private Double _lattitude;
    private Double _longitude;

    @DynamoDBHashKey(attributeName = "gridLocation")
    @DynamoDBAttribute(attributeName = "gridLocation")
    public String getGridLocation() {
        return _gridLocation;
    }

    public void setGridLocation(final String _gridLocation) {
        this._gridLocation = _gridLocation;
    }
    @DynamoDBRangeKey(attributeName = "markerID")
    @DynamoDBAttribute(attributeName = "markerID")
    public String getMarkerID() {
        return _markerID;
    }

    public void setMarkerID(final String _markerID) {
        this._markerID = _markerID;
    }
    @DynamoDBAttribute(attributeName = "lattitude")
    public Double getLattitude() {
        return _lattitude;
    }

    public void setLattitude(final Double _lattitude) {
        this._lattitude = _lattitude;
    }
    @DynamoDBAttribute(attributeName = "longitude")
    public Double getLongitude() {
        return _longitude;
    }

    public void setLongitude(final Double _longitude) {
        this._longitude = _longitude;
    }

}
