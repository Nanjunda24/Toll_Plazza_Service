package com.tollplaza.service;

import com.tollplaza.config.AppProperties;
import com.tollplaza.dto.TollPlazaDto;
import com.tollplaza.model.GeoPoint;
import com.tollplaza.model.RouteInfo;
import com.tollplaza.model.TollPlaza;
import com.tollplaza.repository.TollPlazaRepository;
import com.tollplaza.util.GeoUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Decides which toll plazas lie on a route.
 * <ol>
 *   <li>Bounding-box query in the DB (route bbox + margin) to shrink the candidate set.</li>
 *   <li>For each candidate, find the nearest point on the polyline.</li>
 *   <li>Keep it if that distance is within the configured threshold; its distance from the source
 *       is the cumulative length along the route up to that point.</li>
 * </ol>
 */
@Component
public class TollPlazaMatcher {

    private static final double METERS_PER_DEGREE = 111_000.0;

    private final TollPlazaRepository repository;
    private final double maxDistanceMeters;

    public TollPlazaMatcher(TollPlazaRepository repository, AppProperties props) {
        this.repository = repository;
        this.maxDistanceMeters = props.matching().maxDistanceMeters();
    }

    public List<TollPlazaDto> match(RouteInfo route) {
        List<GeoPoint> line = route.geometry();
        if (line.size() < 2) {
            return List.of();
        }

        double[] cumulative = cumulativeDistances(line);
        double totalPolylineLength = cumulative[cumulative.length - 1];
        // OSRM's reported distance and the polyline length differ by a hair; scale so the last plaza
        // can never be reported beyond the route's total distance.
        double scale = totalPolylineLength == 0 ? 1 : route.distanceMeters() / totalPolylineLength;

        List<TollPlaza> candidates = loadCandidates(line);
        List<TollPlazaDto> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (TollPlaza plaza : candidates) {
            GeoPoint p = new GeoPoint(plaza.getLatitude(), plaza.getLongitude());
            double bestDist = Double.MAX_VALUE;
            double bestAlong = 0;
            for (int i = 0; i < line.size() - 1; i++) {
                GeoUtils.Projection proj = GeoUtils.projectOnSegment(p, line.get(i), line.get(i + 1));
                if (proj.distanceMeters() < bestDist) {
                    bestDist = proj.distanceMeters();
                    bestAlong = cumulative[i] + proj.fraction() * (cumulative[i + 1] - cumulative[i]);
                }
            }
            if (bestDist <= maxDistanceMeters) {
                // The CSV lists many plazas once per direction; collapse near-identical ones.
                String dedupeKey = plaza.getName().trim().toLowerCase(Locale.ROOT) + "|" + Math.round(bestAlong / 500.0);
                if (seen.add(dedupeKey)) {
                    result.add(new TollPlazaDto(plaza.getName(), plaza.getLatitude(), plaza.getLongitude(),
                            round2(bestAlong * scale / 1000.0)));
                }
            }
        }
        result.sort(Comparator.comparingDouble(TollPlazaDto::distanceFromSource));
        return result;
    }

    private List<TollPlaza> loadCandidates(List<GeoPoint> line) {
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        for (GeoPoint g : line) {
            minLat = Math.min(minLat, g.latitude());
            maxLat = Math.max(maxLat, g.latitude());
            minLon = Math.min(minLon, g.longitude());
            maxLon = Math.max(maxLon, g.longitude());
        }
        double margin = maxDistanceMeters / METERS_PER_DEGREE * 1.5;
        return repository.findWithinBoundingBox(minLat - margin, maxLat + margin, minLon - margin, maxLon + margin);
    }

    private static double[] cumulativeDistances(List<GeoPoint> line) {
        double[] cum = new double[line.size()];
        for (int i = 1; i < line.size(); i++) {
            cum[i] = cum[i - 1] + GeoUtils.haversine(line.get(i - 1), line.get(i));
        }
        return cum;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
