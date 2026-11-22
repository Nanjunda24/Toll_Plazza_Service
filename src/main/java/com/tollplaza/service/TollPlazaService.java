package com.tollplaza.service;

import com.tollplaza.client.GeocodingClient;
import com.tollplaza.client.RoutingClient;
import com.tollplaza.dto.RouteSummary;
import com.tollplaza.dto.TollPlazaDto;
import com.tollplaza.dto.TollPlazaResponse;
import com.tollplaza.exception.InvalidPincodeException;
import com.tollplaza.exception.SamePincodeException;
import com.tollplaza.model.GeoPoint;
import com.tollplaza.model.RouteInfo;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class TollPlazaService {

    private static final Pattern PINCODE = Pattern.compile("^[1-9][0-9]{5}$");

    private final GeocodingClient geocodingClient;
    private final RoutingClient routingClient;
    private final TollPlazaMatcher matcher;

    public TollPlazaService(GeocodingClient geocodingClient, RoutingClient routingClient, TollPlazaMatcher matcher) {
        this.geocodingClient = geocodingClient;
        this.routingClient = routingClient;
        this.matcher = matcher;
    }

    /**
     * Whole result is cached per (source, destination) so repeat calls skip geocoding, routing and matching.
     * Exceptions are never cached.
     */
    @Cacheable(cacheNames = "tollPlazas", key = "#source + '-' + #destination")
    public TollPlazaResponse findTollPlazas(String source, String destination) {
        validate(source, destination);

        GeoPoint from = geocodingClient.geocode(source);
        GeoPoint to = geocodingClient.geocode(destination);
        RouteInfo route = routingClient.route(from, to);

        List<TollPlazaDto> plazas = matcher.match(route);
        double km = Math.round(route.distanceMeters() / 10.0) / 100.0;
        return new TollPlazaResponse(new RouteSummary(source, destination, km), plazas);
    }

    private void validate(String source, String destination) {
        if (source == null || destination == null
                || !PINCODE.matcher(source).matches() || !PINCODE.matcher(destination).matches()) {
            throw new InvalidPincodeException("Invalid source or destination pincode");
        }
        if (source.equals(destination)) {
            throw new SamePincodeException();
        }
    }
}
