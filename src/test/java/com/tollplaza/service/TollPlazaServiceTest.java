package com.tollplaza.service;

import com.tollplaza.client.GeocodingClient;
import com.tollplaza.client.RoutingClient;
import com.tollplaza.dto.TollPlazaDto;
import com.tollplaza.dto.TollPlazaResponse;
import com.tollplaza.exception.InvalidPincodeException;
import com.tollplaza.exception.SamePincodeException;
import com.tollplaza.model.GeoPoint;
import com.tollplaza.model.RouteInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TollPlazaServiceTest {

    @Mock GeocodingClient geocoder;
    @Mock RoutingClient router;
    @Mock TollPlazaMatcher matcher;

    TollPlazaService service;

    @BeforeEach
    void setUp() {
        service = new TollPlazaService(geocoder, router, matcher);
    }

    @Test
    void returnsRouteAndTollPlazas() {
        GeoPoint a = new GeoPoint(28.6, 77.2);
        GeoPoint b = new GeoPoint(12.9, 77.6);
        RouteInfo route = new RouteInfo(2_100_456, List.of(a, b));
        List<TollPlazaDto> plazas = List.of(new TollPlazaDto("Plaza 1", 27.0, 77.3, 200.5));
        when(geocoder.geocode("110001")).thenReturn(a);
        when(geocoder.geocode("560001")).thenReturn(b);
        when(router.route(a, b)).thenReturn(route);
        when(matcher.match(route)).thenReturn(plazas);

        TollPlazaResponse response = service.findTollPlazas("110001", "560001");

        assertThat(response.route().sourcePincode()).isEqualTo("110001");
        assertThat(response.route().destinationPincode()).isEqualTo("560001");
        assertThat(response.route().distanceInKm()).isEqualTo(2100.46);
        assertThat(response.tollPlazas()).isEqualTo(plazas);
    }

    @Test
    void noTollPlazasStillReturnsRoute() {
        GeoPoint a = new GeoPoint(1, 1);
        GeoPoint b = new GeoPoint(2, 2);
        RouteInfo route = new RouteInfo(10_000, List.of(a, b));
        when(geocoder.geocode(any())).thenReturn(a, b);
        when(router.route(a, b)).thenReturn(route);
        when(matcher.match(route)).thenReturn(List.of());

        TollPlazaResponse response = service.findTollPlazas("110001", "110002");

        assertThat(response.tollPlazas()).isEmpty();
        assertThat(response.route().distanceInKm()).isEqualTo(10.0);
    }

    @Test
    void samePincodeIsRejectedBeforeAnyExternalCall() {
        assertThatThrownBy(() -> service.findTollPlazas("560001", "560001"))
                .isInstanceOf(SamePincodeException.class)
                .hasMessage("Source and destination pincodes cannot be the same");
        verifyNoInteractions(geocoder, router, matcher);
    }

    @Test
    void malformedPincodesAreRejected() {
        for (String bad : new String[]{"12345", "1234567", "abcdef", "012345", "", " 110001"}) {
            assertThatThrownBy(() -> service.findTollPlazas(bad, "560001"))
                    .as("source=%s", bad)
                    .isInstanceOf(InvalidPincodeException.class);
            assertThatThrownBy(() -> service.findTollPlazas("560001", bad))
                    .as("destination=%s", bad)
                    .isInstanceOf(InvalidPincodeException.class);
        }
        assertThatThrownBy(() -> service.findTollPlazas(null, "560001")).isInstanceOf(InvalidPincodeException.class);
        verifyNoInteractions(geocoder, router, matcher);
    }

    @Test
    void unknownPincodeFromGeocoderPropagates() {
        when(geocoder.geocode("999999")).thenThrow(new InvalidPincodeException("Invalid source or destination pincode"));
        assertThatThrownBy(() -> service.findTollPlazas("999999", "560001"))
                .isInstanceOf(InvalidPincodeException.class);
    }
}
