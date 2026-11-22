package com.tollplaza.util;

import com.tollplaza.model.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/** Decodes the Google/OSRM encoded polyline format (precision 5). */
public final class PolylineDecoder {

    private PolylineDecoder() {
    }

    public static List<GeoPoint> decode(String encoded) {
        List<GeoPoint> points = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return points;
        }
        int index = 0;
        int lat = 0;
        int lon = 0;
        while (index < encoded.length()) {
            int[] r = readValue(encoded, index);
            lat += r[0];
            index = r[1];
            r = readValue(encoded, index);
            lon += r[0];
            index = r[1];
            points.add(new GeoPoint(lat / 1e5, lon / 1e5));
        }
        return points;
    }

    private static int[] readValue(String s, int index) {
        int result = 0;
        int shift = 0;
        int b;
        do {
            if (index >= s.length()) {
                throw new IllegalArgumentException("Malformed polyline");
            }
            b = s.charAt(index++) - 63;
            result |= (b & 0x1f) << shift;
            shift += 5;
        } while (b >= 0x20);
        int value = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
        return new int[]{value, index};
    }
}
