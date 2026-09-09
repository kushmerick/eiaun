package io.eiaun.physics;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
public class Location {

    public static final Location ORIGIN = Location.of(0, 0);

    private final int lat;
    private final int lon;

    private Location(int lat, int lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public static Location of(int lat, int lon) {
        return new Location(lat, lon);
    }

    public static int wrap(int index, int grid) {
        return ((index % grid) + grid) % grid;
    }

    public Location add(Location that, int grid) {
        return Location.of(
                wrap(this.lat + that.lat, grid),
                wrap(this.lon + that.lon, grid));
    }

    public double distanceFromOrigin() {
        return distance(ORIGIN);
    }

    public double distance(Location that) {
        double a = this.lat - that.lat;
        double b = this.lon - that.lon;
        return Math.sqrt(a * a + b * b);
    }

    public String toString() {
        return String.format("{%d,%d}", this.lat, this.lon);
    }

}
