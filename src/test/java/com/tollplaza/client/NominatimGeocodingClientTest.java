package com.tollplaza.client;

import com.tollplaza.exception.ExternalServiceException;
import com.tollplaza.exception.InvalidPincodeException;
import com.tollplaza.model.GeoPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NominatimGeocodingClientTest {

    MockRestServiceServer server;
    NominatimGeocodingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://geo.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new NominatimGeocodingClient(builder.build());
    }

    @Test
    void parsesLatLonFromFirstResult() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://geo.test/search")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("postalcode", "560001"))
                .andExpect(queryParam("country", "India"))
                .andRespond(withSuccess("[{\"lat\":\"12.9767\",\"lon\":\"77.5713\"}]", MediaType.APPLICATION_JSON));

        GeoPoint p = client.geocode("560001");

        assertThat(p.latitude()).isEqualTo(12.9767);
        assertThat(p.longitude()).isEqualTo(77.5713);
        server.verify();
    }

    @Test
    void emptyResultMeansInvalidPincode() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://geo.test/search")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.geocode("999999"))
                .isInstanceOf(InvalidPincodeException.class)
                .hasMessage("Invalid source or destination pincode");
    }

    @Test
    void serverErrorBecomesExternalServiceException() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://geo.test/search")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.geocode("560001")).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void resultWithoutCoordinatesIsTreatedAsUpstreamFailure() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://geo.test/search")))
                .andRespond(withSuccess("[{\"foo\":1}]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.geocode("560001")).isInstanceOf(ExternalServiceException.class);
    }
}
