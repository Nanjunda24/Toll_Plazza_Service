package com.tollplaza.loader;

import com.tollplaza.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/** Runs the CSV upsert at startup (can be switched off with toll.csv.load-on-startup=false). */
@Component
public class TollPlazaDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TollPlazaDataLoader.class);

    private final TollPlazaCsvImporter importer;
    private final ResourceLoader resourceLoader;
    private final AppProperties props;

    public TollPlazaDataLoader(TollPlazaCsvImporter importer, ResourceLoader resourceLoader, AppProperties props) {
        this.importer = importer;
        this.resourceLoader = resourceLoader;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!props.csv().loadOnStartup()) {
            log.info("CSV load on startup disabled");
            return;
        }
        Resource resource = resourceLoader.getResource(props.csv().location());
        if (!resource.exists()) {
            log.warn("Toll plaza CSV not found at {}, skipping import", props.csv().location());
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            importer.importCsv(in);
        }
    }
}
