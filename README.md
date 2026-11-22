# Toll Plazas Between Two Pincodes

Spring Boot REST service that, given a source and destination Indian pincode, returns the toll plazas on the
driving route between them: name, latitude/longitude and distance from the source.

## How it works

```
POST /api/v1/toll-plazas
        │
        ▼
 TollPlazaController ── validates body (@Valid) ──► GlobalExceptionHandler (uniform {"error": ...})
        │
        ▼
 TollPlazaService  (@Cacheable per source+destination)
   1. validate pincodes, reject same source/destination
   2. GeocodingClient  → pincode → lat/lon      (Nominatim / OpenStreetMap, cached per pincode)
   3. RoutingClient    → route geometry + km    (OSRM public server)
   4. TollPlazaMatcher → which plazas lie on the route polyline
        │
        ▼
 TollPlazaRepository (H2, loaded from CSV at startup via upsert)
```

**Finding plazas on the route.** The DB is first filtered with a bounding box around the route, then each
candidate is projected onto the route polyline. A plaza counts as "on the route" if it is within
`toll.matching.max-distance-meters` (default 300 m) of the line. Its distance from source is the length of
the route up to that projection point. The CSV lists most plazas once per direction, so near-identical
entries are collapsed.

**Design notes**
- `GeocodingClient` / `RoutingClient` are interfaces; swap in Mappls or Google Maps by adding another implementation.
- CSV import is a true **upsert**: de-duplicate in memory, one `IN` query for existing rows, update changed rows,
  batch-insert new ones. Re-running never creates duplicates (`TollPlazaCsvImporter`).
- **Caching** (Caffeine, 24 h): full responses per pincode pair, plus geocoding results per pincode. Failures are not cached.
- No API keys needed.

## Requirements
- Java 17+
- Maven 3.9+
- Internet access at runtime (calls Nominatim and OSRM public servers)

## Run

```bash
mvn spring-boot:run
# or
mvn clean package && java -jar target/toll-plaza-service-1.0.0.jar
```

Starts on `http://localhost:8080`. The bundled `toll_plaza_india.csv` is imported on startup.
To use a different file: `--toll.csv.location=file:/path/to/file.csv`
(expected columns: `longitude,latitude,toll_name,geo_state`).

## API

`POST /api/v1/toll-plazas`

```json
{ "sourcePincode": "560064", "destinationPincode": "411045" }
```

Success `200`:
```json
{
  "route": { "sourcePincode": "560064", "destinationPincode": "411045", "distanceInKm": 851.8 },
  "tollPlazas": [
    { "name": "…", "latitude": 13.1, "longitude": 77.5, "distanceFromSource": 42.7 }
  ]
}
```
No plazas on route → `200` with `"tollPlazas": []`.

| Case | Status | Body |
|---|---|---|
| Malformed pincode (not 6 digits, leading 0, letters) | 400 | `{"error":"Invalid source or destination pincode"}` |
| Pincode doesn't exist | 400 | `{"error":"Invalid source or destination pincode"}` |
| Same source and destination | 400 | `{"error":"Source and destination pincodes cannot be the same"}` |
| Missing field | 400 | `{"error":"sourcePincode is required"}` |
| Missing / malformed JSON | 400 | `{"error":"Request body is missing or malformed JSON"}` |
| No driving route | 404 | `{"error":"No driving route found between the given pincodes"}` |
| Geocoder / router down | 502 | `{"error":"... service is unavailable, please try again later"}` |

Import `postman_collection.json` into Postman, or:

```bash
curl -X POST localhost:8080/api/v1/toll-plazas \
  -H 'Content-Type: application/json' \
  -d '{"sourcePincode":"560064","destinationPincode":"411045"}'
```

## Tests

```bash
mvn test            # JaCoCo report: target/site/jacoco/index.html
```

Unit tests: polyline decoding, geometry, matcher, service. Slice tests: controller (MockMvc), HTTP clients
(MockRestServiceServer), CSV upsert (`@DataJpaTest`). Integration: cache behaviour, full startup with real CSV.
External APIs are mocked, so tests run offline.

## Configuration (`application.yml`)

Set `DB_PASSWORD` in the environment to configure the datasource password. It defaults to empty for the
local in-memory H2 database.

| Property | Default |
|---|---|
| `toll.matching.max-distance-meters` | 300 |
| `toll.routing.base-url` | `https://router.project-osrm.org` |
| `toll.geocoding.base-url` | `https://nominatim.openstreetmap.org` |
| `toll.csv.location` | `classpath:data/toll_plaza_india.csv` |
| `toll.csv.load-on-startup` | true |

## Known limitations
- Public OSRM/Nominatim servers are rate-limited and meant for light use; point the base URLs at your own instance for production.
- Nominatim returns one centroid per pincode, so route endpoints are approximate.
- The route is OSRM's fastest route, which can differ from Mappls' default, so expect most (not necessarily all) plazas to match.
