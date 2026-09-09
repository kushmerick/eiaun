package io.eiaun.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocationTest {

    @Test
    void canCalculateDistanceFromOrigin() {
        int lat = 123;
        int lon = 456;
        Location location = Location.of(lat, lon);
        double distance = Math.sqrt(Math.pow(lat, 2) + Math.pow(lon, 2));
        assertEquals(distance, location.distanceFromOrigin());
        assertEquals(distance, location.distance(Location.ORIGIN));
        assertEquals(distance, Location.ORIGIN.distance(location));
        assertEquals(0, Location.ORIGIN.distanceFromOrigin());
    }

    @Test
    void canCalculateDistance() {
        int lat1 = 123;
        int lon1 = 456;
        int lat2 = 789;
        int lon2 = 101112;
        double distance = Math.sqrt(Math.pow(lat1 - lat2, 2) + Math.pow(lon1 - lon2, 2));
        assertEquals(distance, Location.of(lat1, lon1).distance(Location.of(lat2, lon2)));
        assertEquals(distance, Location.of(lat2, lon2).distance(Location.of(lat1, lon1)));
    }

}