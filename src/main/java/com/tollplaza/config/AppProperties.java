package com.tollplaza.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration, bound from the "toll" prefix in application.yml.
 */
@ConfigurationProperties(prefix = "toll")
public record AppProperties(Geocoding geocoding, Routing routing, Matching matching, Csv csv) {

    public record Geocoding(String baseUrl, String userAgent, int timeoutSeconds) {
    }

    public record Routing(String baseUrl, int timeoutSeconds) {
    }

    /** maxDistanceMeters: how far a plaza may sit from the route line and still count as "on the route". */
    public record Matching(double maxDistanceMeters) {
    }

    /** location: Spring resource path, e.g. classpath:data/toll_plaza_india.csv or file:/path/to/file.csv */
    public record Csv(String location, boolean loadOnStartup) {
    }
}
