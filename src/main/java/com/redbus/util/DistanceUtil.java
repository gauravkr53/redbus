package com.redbus.util;

import com.redbus.model.City;

public class DistanceUtil {
    private static final double EARTH_RADIUS_KM = 6371.0;

    public static double calculateDistance(City source, City destination) {
        double lat1Rad = Math.toRadians(source.getLatitude());
        double lat2Rad = Math.toRadians(destination.getLatitude());
        double deltaLatRad = Math.toRadians(destination.getLatitude() - source.getLatitude());
        double deltaLonRad = Math.toRadians(destination.getLongitude() - source.getLongitude());

        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
}
