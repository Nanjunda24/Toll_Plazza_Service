package com.tollplaza.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.tollplaza.config.AppProperties;
import com.tollplaza.exception.ExternalServiceException;
import com.tollplaza.exception.RouteNotFoundException;
import com.tollplaza.model.GeoPoint;
import com.tollplaza.model.RouteInfo;
import com.tollplaza.util.PolylineDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/** Gets driving routes (distance + full geometry) from an OSRM server. */
@Component
public class OsrmRoutingClient implements RoutingClient {

    private static final Logger log = LoggerFactory.getLogger(OsrmRoutingClient.class);

    private final RestClient restClient;

    @Autowired
    public OsrmRoutingClient(RestClient.Builder builder, AppProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(props.routing().timeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(props.routing().timeoutSeconds()));
        this.restClient = builder.baseUrl(props.routing().baseUrl()).requestFactory(factory).build();
    }

    /** Constructor for tests, which supply a pre-configured RestClient. */
    OsrmRoutingClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public RouteInfo route(GeoPoint from, GeoPoint to) {
        // OSRM expects lon,lat order
        String coords = String.format(Locale.ROOT, "%.6f,%.6f;%.6f,%.6f",
                from.longitude(), from.latitude(), to.longitude(), to.latitude());
        JsonNode body;
        try {
            body = restClient.get()
                    .uri("/route/v1/driving/{coords}?overview=full&geometries=polyline&steps=false", coords)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.error("Routing call failed", e);
            throw new ExternalServiceException("Routing service is unavailable, please try again later", e);
        }

        if (body == null || !"Ok".equals(body.path("code").asText())) {
            throw new RouteNotFoundException("No driving route found between the given pincodes");
        }
        JsonNode route = body.path("routes").path(0);
        if (route.isMissingNode() || !route.has("geometry")) {
            throw new RouteNotFoundException("No driving route found between the given pincodes");
        }
        List<GeoPoint> geometry;
        try {
            geometry = PolylineDecoder.decode(route.get("geometry").asText());
        } catch (IllegalArgumentException e) {
            throw new ExternalServiceException("Routing service returned an invalid geometry", e);
        }
        return new RouteInfo(route.path("distance").asDouble(), geometry);
    }
}
