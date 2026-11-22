package com.tollplaza.loader;

import com.tollplaza.repository.TollPlazaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TollPlazaCsvImporter.class)
class TollPlazaCsvImporterTest {

    @Autowired TollPlazaCsvImporter importer;
    @Autowired TollPlazaRepository repository;

    private static InputStream csv(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    private static final String HEADER = "longitude,latitude,toll_name,geo_state\n";

    @Test
    void insertsRowsAndCollapsesDuplicatesInsideTheFile() throws IOException {
        var result = importer.importCsv(csv(HEADER
                + "77.485,23.1415,11 Miles Toll Plaza,Madhya Pradesh\n"
                + "77.485,23.1415,11 Miles Toll Plaza,Madhya Pradesh\n"
                + "79.6149671,22.0757748,Seoni Toll Plaza,Madhya Pradesh\n"));

        assertThat(result.rowsRead()).isEqualTo(3);
        assertThat(result.inserted()).isEqualTo(2);
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void reimportingTheSameFileIsIdempotent() throws IOException {
        String data = HEADER + "77.485,23.1415,11 Miles Toll Plaza,Madhya Pradesh\n";
        importer.importCsv(csv(data));

        var second = importer.importCsv(csv(data));

        assertThat(second.inserted()).isZero();
        assertThat(second.updated()).isEqualTo(1);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void existingRowIsUpdatedNotDuplicated() throws IOException {
        importer.importCsv(csv(HEADER + "77.485,23.1415,11 Miles Toll Plaza,MP\n"));
        importer.importCsv(csv(HEADER + "77.485,23.1415,11 Miles Toll Plaza,Madhya Pradesh\n"));

        assertThat(repository.findAll()).singleElement()
                .satisfies(t -> assertThat(t.getState()).isEqualTo("Madhya Pradesh"));
    }

    @Test
    void invalidRowsAreSkippedAndCounted() throws IOException {
        var result = importer.importCsv(csv(HEADER
                + "abc,23.1,Bad Lon Plaza,X\n"
                + "77.4,95.0,Bad Lat Plaza,X\n"
                + "77.4,23.1,,X\n"
                + "77.4,23.1,Good Plaza,X\n"));

        assertThat(result.rowsRead()).isEqualTo(4);
        assertThat(result.skippedInvalid()).isEqualTo(3);
        assertThat(result.inserted()).isEqualTo(1);
    }

    @Test
    void sameNameAtDifferentLocationsAreDistinctPlazas() throws IOException {
        importer.importCsv(csv(HEADER
                + "77.0,12.0,Tumkur Toll Plaza,KA\n"
                + "78.0,13.0,Tumkur Toll Plaza,KA\n"));

        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void boundingBoxQueryOnlyReturnsPlazasInsideBox() throws IOException {
        importer.importCsv(csv(HEADER
                + "77.5,12.5,Inside,KA\n"
                + "80.0,20.0,Outside,MH\n"));

        assertThat(repository.findWithinBoundingBox(12.0, 13.0, 77.0, 78.0))
                .extracting("name").containsExactly("Inside");
    }

    @Test
    void headerOnlyFileImportsNothing() throws IOException {
        var result = importer.importCsv(csv(HEADER));
        assertThat(result.rowsRead()).isZero();
        assertThat(repository.count()).isZero();
    }
}
