package com.tollplaza.util;

import com.tollplaza.model.GeoPoint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class GeoUtilsTest {

    @Test
    void haversineDelhiToBengaluruIsAbout1750Km() {
        double d = GeoUtils.haversine(new GeoPoint(28.6139, 77.2090), new GeoPoint(12.9716, 77.5946));
        assertThat(d / 1000).isBetween(1700.0, 1760.0);
    }

    @Test
    void haversineOfSamePointIsZero() {
        GeoPoint p = new GeoPoint(19.07, 72.87);
        assertThat(GeoUtils.haversine(p, p)).isZero();
    }

    @Test
    void projectionOntoMiddleOfSegment() {
        GeoPoint a = new GeoPoint(12.0, 77.0);
        GeoPoint b = new GeoPoint(12.0, 78.0);
        GeoUtils.Projection p = GeoUtils.projectOnSegment(new GeoPoint(12.001, 77.5), a, b);

        assertThat(p.fraction()).isCloseTo(0.5, within(0.001));
        assertThat(p.distanceMeters()).isCloseTo(111.0, within(2.0));
    }

    @Test
    void projectionBeyondEndIsClampedToEndpoint() {
        GeoPoint a = new GeoPoint(12.0, 77.0);
        GeoPoint b = new GeoPoint(12.0, 77.1);
        GeoUtils.Projection p = GeoUtils.projectOnSegment(new GeoPoint(12.0, 77.2), a, b);

        assertThat(p.fraction()).isEqualTo(1.0);
        assertThat(p.distanceMeters()).isGreaterThan(10_000);
    }

    @Test
    void degenerateSegmentDoesNotDivideByZero() {
        GeoPoint a = new GeoPoint(12.0, 77.0);
        GeoUtils.Projection p = GeoUtils.projectOnSegment(new GeoPoint(12.001, 77.0), a, a);
        assertThat(p.fraction()).isZero();
        assertThat(p.distanceMeters()).isCloseTo(111.0, within(2.0));
    }
}
