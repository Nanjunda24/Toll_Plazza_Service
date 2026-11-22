package com.tollplaza.controller;

import com.tollplaza.dto.RouteSummary;
import com.tollplaza.dto.TollPlazaDto;
import com.tollplaza.dto.TollPlazaResponse;
import com.tollplaza.exception.ExternalServiceException;
import com.tollplaza.exception.InvalidPincodeException;
import com.tollplaza.exception.RouteNotFoundException;
import com.tollplaza.exception.SamePincodeException;
import com.tollplaza.service.TollPlazaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({TollPlazaController.class, ApiInfoController.class})
class TollPlazaControllerTest {

    private static final String URL = "/api/v1/toll-plazas";

    @Autowired MockMvc mvc;
    @MockBean TollPlazaService service;

    private static String body(String s, String d) {
        return "{\"sourcePincode\":" + (s == null ? "null" : "\"" + s + "\"")
                + ",\"destinationPincode\":" + (d == null ? "null" : "\"" + d + "\"") + "}";
    }

    @Test
    void successMatchesSpecShape() throws Exception {
        when(service.findTollPlazas("110001", "560001")).thenReturn(new TollPlazaResponse(
                new RouteSummary("110001", "560001", 2100),
                List.of(new TollPlazaDto("Toll Plaza 1", 28.7041, 77.1025, 200))));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("110001", "560001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route.sourcePincode").value("110001"))
                .andExpect(jsonPath("$.route.destinationPincode").value("560001"))
                .andExpect(jsonPath("$.route.distanceInKm").value(2100))
                .andExpect(jsonPath("$.tollPlazas[0].name").value("Toll Plaza 1"))
                .andExpect(jsonPath("$.tollPlazas[0].latitude").value(28.7041))
                .andExpect(jsonPath("$.tollPlazas[0].longitude").value(77.1025))
                .andExpect(jsonPath("$.tollPlazas[0].distanceFromSource").value(200));
    }

    @Test
    void noTollPlazasReturns200WithEmptyList() throws Exception {
        when(service.findTollPlazas("110001", "110002")).thenReturn(
                new TollPlazaResponse(new RouteSummary("110001", "110002", 5), List.of()));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("110001", "110002")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tollPlazas").isEmpty());
    }

    @Test
    void malformedPincodeIs400WithSpecMessage() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("11000", "560001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid source or destination pincode"));
        verifyNoInteractions(service);
    }

    @Test
    void nonNumericPincodeIs400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("abcdef", "560001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid source or destination pincode"));
    }

    @Test
    void missingFieldIs400WithHelpfulMessage() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body(null, "560001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("sourcePincode is required"));
    }

    @Test
    void emptyBodyIs400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void samePincodeIs400() throws Exception {
        when(service.findTollPlazas("560001", "560001")).thenThrow(new SamePincodeException());

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("560001", "560001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Source and destination pincodes cannot be the same"));
    }

    @Test
    void unknownPincodeIs400() throws Exception {
        when(service.findTollPlazas("999999", "560001"))
                .thenThrow(new InvalidPincodeException("Invalid source or destination pincode"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("999999", "560001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid source or destination pincode"));
    }

    @Test
    void noRouteIs404() throws Exception {
        when(service.findTollPlazas(anyString(), anyString())).thenThrow(new RouteNotFoundException("No driving route found"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("110001", "682001")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No driving route found"));
    }

    @Test
    void upstreamFailureIs502() throws Exception {
        when(service.findTollPlazas(anyString(), anyString())).thenThrow(new ExternalServiceException("Routing service is unavailable"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("110001", "560001")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Routing service is unavailable"));
    }

    @Test
    void unexpectedErrorIs500WithoutLeakingDetails() throws Exception {
        when(service.findTollPlazas(anyString(), anyString())).thenThrow(new IllegalStateException("secret detail"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("110001", "560001")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Something went wrong, please try again later"));
    }

    @Test
    void wrongMethodIs405() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(URL))
                .andExpect(status().isMethodNotAllowed());
    }

        @Test
        void rootShowsApiInfo() throws Exception {
                mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Toll Plaza API is running"))
                                .andExpect(jsonPath("$.endpoint").value(URL))
                                .andExpect(jsonPath("$.method").value("POST"));
        }
}
