package com.tollplaza.loader;

import com.tollplaza.model.TollPlaza;
import com.tollplaza.repository.TollPlazaRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Imports toll plazas from CSV (columns: longitude, latitude, toll_name, geo_state) as an <b>upsert</b>:
 * rows are de-duplicated in-memory, existing rows are found with one IN-query, changed ones are updated,
 * new ones are inserted in a single saveAll batch. Running it twice never creates duplicates.
 */
@Component
public class TollPlazaCsvImporter {

    private static final Logger log = LoggerFactory.getLogger(TollPlazaCsvImporter.class);

    private final TollPlazaRepository repository;

    public TollPlazaCsvImporter(TollPlazaRepository repository) {
        this.repository = repository;
    }

    public record ImportResult(int rowsRead, int skippedInvalid, int inserted, int updated) {
    }

    @Transactional
    public ImportResult importCsv(InputStream in) throws IOException {
        Map<String, TollPlaza> parsed = new LinkedHashMap<>();
        int rows = 0;
        int invalid = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true).setTrim(true).setIgnoreEmptyLines(true)
                .build();
        try (CSVParser parser = format.parse(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            for (CSVRecord r : parser) {
                rows++;
                TollPlaza plaza = toEntity(r);
                if (plaza == null) {
                    invalid++;
                    continue;
                }
                parsed.putIfAbsent(plaza.getNaturalKey(), plaza); // drops exact duplicates inside the file
            }
        }

        Map<String, TollPlaza> existing = repository.findByNaturalKeyIn(parsed.keySet()).stream()
                .collect(Collectors.toMap(TollPlaza::getNaturalKey, Function.identity()));

        List<TollPlaza> toInsert = new ArrayList<>();
        int updated = 0;
        for (TollPlaza incoming : parsed.values()) {
            TollPlaza current = existing.get(incoming.getNaturalKey());
            if (current == null) {
                toInsert.add(incoming);
            } else {
                current.updateFrom(incoming.getName(), incoming.getLatitude(), incoming.getLongitude(), incoming.getState());
                updated++;
            }
        }
        repository.saveAll(toInsert);

        ImportResult result = new ImportResult(rows, invalid, toInsert.size(), updated);
        log.info("Toll plaza import finished: {}", result);
        return result;
    }

    private TollPlaza toEntity(CSVRecord r) {
        try {
            String name = r.get("toll_name");
            String state = r.isSet("geo_state") ? r.get("geo_state") : null;
            double lat = Double.parseDouble(r.get("latitude"));
            double lon = Double.parseDouble(r.get("longitude"));
            if (name == null || name.isBlank() || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                return null;
            }
            return new TollPlaza(name.trim(), lat, lon, state == null || state.isBlank() ? null : state.trim());
        } catch (IllegalArgumentException | IllegalStateException e) {
            // covers NumberFormatException, missing column, short row
            log.warn("Skipping invalid CSV row {}: {}", r.getRecordNumber(), e.getMessage());
            return null;
        }
    }
}
