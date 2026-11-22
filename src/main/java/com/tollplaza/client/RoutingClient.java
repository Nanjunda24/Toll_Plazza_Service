package com.tollplaza.client;

import com.tollplaza.model.GeoPoint;
import com.tollplaza.model.RouteInfo;

/** Computes a driving route between two points. Implementations are swappable (OSRM, Google, Mappls...). */
public interface RoutingClient {

    RouteInfo route(GeoPoint from, GeoPoint to);
}
