package com.tollplaza.client;

import com.tollplaza.exception.ExternalServiceException;
import com.tollplaza.exception.RouteNotFoundException;
import com.tollplaza.model.GeoPoint;
import com.tollplaza.model.RouteInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OsrmRoutingClientTest {

    static final GeoPoint FROM = new GeoPoint(28.6, 77.2);
    static final GeoPoint TO = new GeoPoint(12.9, 77.6);

    MockRestServiceServer server;
    OsrmRoutingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://osrm.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OsrmRoutingClient(builder.build());
    }

    @Test
    void parsesDistanceAndGeometryAndSendsLonLatOrder() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://osrm.test/route/v1/driving/77.200000%2C28.600000%3B77.600000%2C12.900000")))
                .andRespond(withSuccess("""
                        {"code":"Ok","routes":[{"distance":2100000.5,"geometry":"_p~iF~ps|U_ulLnnqC_mqNvxq`@"}]}
                        """, MediaType.APPLICATION_JSON));

        RouteInfo route = client.route(FROM, TO);

        assertThat(route.distanceMeters()).isEqualTo(2100000.5);
        assertThat(route.geometry()).hasSize(3);
        server.verify();
    }

    @Test
    void noRouteCodeBecomesRouteNotFound() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://osrm.test/")))
                .andRespond(withSuccess("{\"code\":\"NoRoute\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.route(FROM, TO)).isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void okWithoutRoutesBecomesRouteNotFound() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://osrm.test/")))
                .andRespond(withSuccess("{\"code\":\"Ok\",\"routes\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.route(FROM, TO)).isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void serverErrorBecomesExternalServiceException() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://osrm.test/")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.route(FROM, TO)).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void corruptGeometryBecomesExternalServiceException() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://osrm.test/")))
                .andRespond(withSuccess("{\"code\":\"Ok\",\"routes\":[{\"distance\":1,\"geometry\":\"_p~iF~ps|U_\"}]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.route(FROM, TO)).isInstanceOf(ExternalServiceException.class);
    }
}
