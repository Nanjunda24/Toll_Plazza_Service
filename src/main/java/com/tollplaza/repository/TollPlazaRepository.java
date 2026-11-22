package com.tollplaza.repository;

import com.tollplaza.model.TollPlaza;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TollPlazaRepository extends JpaRepository<TollPlaza, Long> {

    /** Used by the CSV upsert: one query to fetch every row that may already exist. */
    List<TollPlaza> findByNaturalKeyIn(Collection<String> naturalKeys);

    /** Cheap bounding-box pre-filter so we never run the geometry maths on the whole table. */
    @Query("""
            select t from TollPlaza t
            where t.latitude between :minLat and :maxLat
              and t.longitude between :minLon and :maxLon
            """)
    List<TollPlaza> findWithinBoundingBox(@Param("minLat") double minLat,
                                          @Param("maxLat") double maxLat,
                                          @Param("minLon") double minLon,
                                          @Param("maxLon") double maxLon);
}
