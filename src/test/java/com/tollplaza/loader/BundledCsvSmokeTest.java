package com.tollplaza.loader;

import com.tollplaza.repository.TollPlazaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Starts the whole app with the real bundled CSV and checks it was imported. */
@SpringBootTest
class BundledCsvSmokeTest {

    @Autowired TollPlazaRepository repository;

    @Test
    void bundledCsvIsLoadedOnStartup() {
        assertThat(repository.count()).isGreaterThan(1000);
    }
}
