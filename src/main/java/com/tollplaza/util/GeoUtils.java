package com.tollplaza.util;

import com.tollplaza.model.GeoPoint;

/** Small, dependency-free geometry helpers. All distances are in metres. */
public final class GeoUtils {

    private static final double EARTH_RADIUS_M = 6_371_000.0;
    private static final double METERS_PER_DEGREE_LAT = Math.PI * EARTH_RADIUS_M / 180.0;

    private GeoUtils() {
    }

    public static double haversine(GeoPoint a, GeoPoint b) {
        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_M * Math.asin(Math.min(1.0, Math.sqrt(h)));
    }

    /**
     * Projection of a point on a segment.
     *
     * @param distanceMeters  distance from the point to the segment
     * @param fraction        0..1, where on the segment the closest point lies
     */
    public record Projection(double distanceMeters, double fraction) {
    }

    /**
     * Projects {@code p} onto segment a-b using a local flat-earth approximation,
     * which is accurate to well under a metre at the segment sizes a road polyline has.
     */
    public static Projection projectOnSegment(GeoPoint p, GeoPoint a, GeoPoint b) {
        double cosLat = Math.cos(Math.toRadians(a.latitude()));
        double mx = METERS_PER_DEGREE_LAT * cosLat;
        double my = METERS_PER_DEGREE_LAT;

        double bx = (b.longitude() - a.longitude()) * mx;
        double by = (b.latitude() - a.latitude()) * my;
        double px = (p.longitude() - a.longitude()) * mx;
        double py = (p.latitude() - a.latitude()) * my;

        double lenSq = bx * bx + by * by;
        double t = lenSq == 0 ? 0 : Math.max(0, Math.min(1, (px * bx + py * by) / lenSq));
        double dx = px - t * bx;
        double dy = py - t * by;
        return new Projection(Math.sqrt(dx * dx + dy * dy), t);
    }
}
