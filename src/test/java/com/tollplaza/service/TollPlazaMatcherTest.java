package com.tollplaza.service;

import com.tollplaza.config.AppProperties;
import com.tollplaza.dto.TollPlazaDto;
import com.tollplaza.model.GeoPoint;
import com.tollplaza.model.RouteInfo;
import com.tollplaza.model.TollPlaza;
import com.tollplaza.repository.TollPlazaRepository;
import com.tollplaza.util.GeoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TollPlazaMatcherTest {

    @Mock
    TollPlazaRepository repository;

    TollPlazaMatcher matcher;

    // Straight east-west road at 12N from 77E to 78E, with a mid point
    final GeoPoint start = new GeoPoint(12.0, 77.0);
    final GeoPoint mid = new GeoPoint(12.0, 77.5);
    final GeoPoint end = new GeoPoint(12.0, 78.0);
    RouteInfo route;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(null, null, new AppProperties.Matching(300), null);
        matcher = new TollPlazaMatcher(repository, props);
        double len = GeoUtils.haversine(start, mid) + GeoUtils.haversine(mid, end);
        route = new RouteInfo(len, List.of(start, mid, end));
    }

    private void candidates(TollPlaza... plazas) {
        when(repository.findWithinBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(plazas));
    }

    @Test
    void keepsPlazaNearRouteAndReportsDistanceAlongRoute() {
        candidates(new TollPlaza("Mid Plaza", 12.001, 77.5, "KA"));

        List<TollPlazaDto> result = matcher.match(route);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Mid Plaza");
        double halfKm = route.distanceMeters() / 2000.0;
        assertThat(result.get(0).distanceFromSource()).isBetween(halfKm - 0.5, halfKm + 0.5);
    }

    @Test
    void dropsPlazaFarFromRoute() {
        candidates(new TollPlaza("Far Plaza", 12.05, 77.5, "KA")); // ~5.5 km off
        assertThat(matcher.match(route)).isEmpty();
    }

    @Test
    void resultsAreSortedBySourceDistance() {
        candidates(
                new TollPlaza("Late", 12.0, 77.9, "KA"),
                new TollPlaza("Early", 12.0, 77.1, "KA"),
                new TollPlaza("Middle", 12.0, 77.5, "KA"));

        List<TollPlazaDto> result = matcher.match(route);

        assertThat(result).extracting(TollPlazaDto::name).containsExactly("Early", "Middle", "Late");
    }

    @Test
    void directionalDuplicatesAreCollapsed() {
        candidates(
                new TollPlaza("Twin Toll Plaza", 12.0002, 77.5, "KA"),
                new TollPlaza("Twin Toll Plaza", 11.9998, 77.5001, "KA"));

        assertThat(matcher.match(route)).hasSize(1);
    }

    @Test
    void noCandidatesGivesEmptyList() {
        candidates();
        assertThat(matcher.match(route)).isEmpty();
    }

    @Test
    void degenerateGeometryDoesNotTouchTheDatabase() {
        assertThat(matcher.match(new RouteInfo(0, List.of(start)))).isEmpty();
        verifyNoInteractions(repository);
    }

    @Test
    void lastPlazaNeverExceedsTotalRouteDistance() {
        candidates(new TollPlaza("Near End", 12.0, 77.999, "KA"));
        List<TollPlazaDto> result = matcher.match(route);
        assertThat(result.get(0).distanceFromSource()).isLessThanOrEqualTo(route.distanceMeters() / 1000.0);
    }
}
