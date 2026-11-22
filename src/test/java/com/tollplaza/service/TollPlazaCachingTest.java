package com.tollplaza.service;

import com.tollplaza.client.GeocodingClient;
import com.tollplaza.client.RoutingClient;
import com.tollplaza.exception.ExternalServiceException;
import com.tollplaza.model.GeoPoint;
import com.tollplaza.model.RouteInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Proves the "use caching for subsequent API calls" requirement against the real Spring cache proxy. */
@SpringBootTest
@ActiveProfiles("test")
class TollPlazaCachingTest {

    @Autowired TollPlazaService service;
    @MockBean GeocodingClient geocoder;
    @MockBean RoutingClient router;

    @Test
    void secondCallWithSamePincodesIsServedFromCache() {
        GeoPoint a = new GeoPoint(28.6, 77.2);
        GeoPoint b = new GeoPoint(12.9, 77.6);
        when(geocoder.geocode("110011")).thenReturn(a);
        when(geocoder.geocode("560011")).thenReturn(b);
        when(router.route(any(), any())).thenReturn(new RouteInfo(1000, List.of(a, b)));

        var first = service.findTollPlazas("110011", "560011");
        var second = service.findTollPlazas("110011", "560011");

        assertThat(second).isSameAs(first);
        verify(router, times(1)).route(any(), any());
    }

    @Test
    void failuresAreNotCached() {
        GeoPoint a = new GeoPoint(1, 1);
        GeoPoint b = new GeoPoint(2, 2);
        when(geocoder.geocode("110022")).thenReturn(a);
        when(geocoder.geocode("560022")).thenReturn(b);
        when(router.route(any(), any()))
                .thenThrow(new ExternalServiceException("down"))
                .thenReturn(new RouteInfo(1000, List.of(a, b)));

        assertThatThrownBy(() -> service.findTollPlazas("110022", "560022")).isInstanceOf(ExternalServiceException.class);
        assertThat(service.findTollPlazas("110022", "560022").route().distanceInKm()).isEqualTo(1.0);
    }
}
