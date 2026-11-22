package com.tollplaza.model;

import java.util.List;

/**
 * A driving route: its total length and the polyline geometry.
 */
public record RouteInfo(double distanceMeters, List<GeoPoint> geometry) {
}
