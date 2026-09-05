package io.eiaun.concepts.ecosystem;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
public class Location {

    private final int lat;
    private final int lon;

    private Location(int lat, int lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public static Location of(int lat, int lon) {
        return new Location(lat, lon);
    }

    public Location add(Location that, int maxLat, int maxLon) {
        return Location.of(
                (this.lat + that.lat) % maxLat,
                (this.lon + that.lon) % maxLon);
    }

}
