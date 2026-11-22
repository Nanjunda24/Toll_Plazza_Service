package com.tollplaza.client;

import com.tollplaza.model.GeoPoint;

/** Resolves an Indian pincode to a coordinate. Implementations are swappable (Nominatim, Mappls, Google...). */
public interface GeocodingClient {

    /** @throws com.tollplaza.exception.InvalidPincodeException if the pincode does not exist */
    GeoPoint geocode(String pincode);
}
