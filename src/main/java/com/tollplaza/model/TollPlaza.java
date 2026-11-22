package com.tollplaza.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.util.Locale;

/**
 * A toll plaza. {@code naturalKey} (name + coordinates rounded to ~1 m) is the upsert key,
 * so re-importing the same CSV never creates duplicates.
 */
@Entity
@Table(name = "toll_plaza", indexes = {
        @Index(name = "idx_toll_lat_lon", columnList = "latitude, longitude")
})
public class TollPlaza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "natural_key", nullable = false, unique = true, length = 300)
    private String naturalKey;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(length = 100)
    private String state;

    protected TollPlaza() {
        // for JPA
    }

    public TollPlaza(String name, double latitude, double longitude, String state) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.state = state;
        this.naturalKey = buildKey(name, latitude, longitude);
    }

    public static String buildKey(String name, double latitude, double longitude) {
        return String.format(Locale.ROOT, "%s|%.5f|%.5f", name.trim().toLowerCase(Locale.ROOT), latitude, longitude);
    }

    public Long getId() {
        return id;
    }

    public String getNaturalKey() {
        return naturalKey;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getState() {
        return state;
    }

    /** Used by the CSV upsert to refresh an existing row (the key stays the same). */
    public void updateFrom(String name, double latitude, double longitude, String state) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.state = state;
    }
}
