package com.tollplaza.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.tollplaza.config.AppProperties;
import com.tollplaza.exception.ExternalServiceException;
import com.tollplaza.exception.InvalidPincodeException;
import com.tollplaza.model.GeoPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * Geocodes pincodes with OpenStreetMap Nominatim (free, no API key).
 * Results are cached because a pincode's location never changes.
 */
@Component
public class NominatimGeocodingClient implements GeocodingClient {

    private static final Logger log = LoggerFactory.getLogger(NominatimGeocodingClient.class);

    private final RestClient restClient;

    @Autowired
    public NominatimGeocodingClient(RestClient.Builder builder, AppProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(props.geocoding().timeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(props.geocoding().timeoutSeconds()));
        this.restClient = builder
                .baseUrl(props.geocoding().baseUrl())
                .defaultHeader("User-Agent", props.geocoding().userAgent())
                .requestFactory(factory)
                .build();
    }

    /** Constructor for tests, which supply a pre-configured RestClient. */
    NominatimGeocodingClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @Cacheable(cacheNames = "geocode", key = "#pincode")
    public GeoPoint geocode(String pincode) {
        JsonNode body;
        try {
            body = restClient.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("postalcode", pincode)
                            .queryParam("country", "India")
                            .queryParam("format", "json")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            log.error("Geocoding call failed for pincode {}", pincode, e);
            throw new ExternalServiceException("Geocoding service is unavailable, please try again later", e);
        }

        if (body == null || !body.isArray() || body.isEmpty()) {
            throw new InvalidPincodeException("Invalid source or destination pincode");
        }
        JsonNode first = body.get(0);
        try {
            return new GeoPoint(first.get("lat").asDouble(), first.get("lon").asDouble());
        } catch (NullPointerException e) {
            throw new ExternalServiceException("Geocoding service returned an unexpected response", e);
        }
    }
}
