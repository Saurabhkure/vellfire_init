# Google Places Data Storage

## 1. Purpose

This document describes the database schema, storage architecture, and
Spring Boot application changes required to persist petrol-station
information returned by the Google Places API.

Google Places data will be stored separately from the existing public
petrol-pump dataset. This keeps the source data independent and allows
both datasets to be matched and enriched in a later phase without
overwriting the original records.

## 2. Scope

The initial implementation covers:

-   Storing Google Place identifiers and place details.
-   Storing coordinates, address, rating, review count, business status,
    and restroom availability when returned.
-   Storing regular weekly opening hours.
-   Updating an existing Google record using its Google Place ID instead
    of inserting duplicates.
-   Recording when Google data was fetched.

Historical rating and business-status tracking is optional and can be
introduced separately if required.

## 3. Data flow architecture

``` text
Google Places API
       |
       v
GooglePlacesService
       |
       v
Response validation and mapping
       |
       v
GooglePlacesStorageService
       |
       +------> GooglePlacesRepository
       |
       +------> GooglePlaceOpeningHoursRepository
       |
       v
PostgreSQL
  - google_places
  - google_place_opening_hours
```

### Processing responsibilities

1.  **GooglePlacesService**
    -   Calls the Google Places API.
    -   Receives the Places response.
    -   Passes the response to the storage layer.
2.  **Response validation and mapping**
    -   Reads the fields returned by Google.
    -   Validates that the result is a petrol station before storing it
        in the petrol-pump dataset.
    -   Maps nested Google response fields into application
        DTOs/entities.
    -   Handles optional or missing fields without assuming they are
        always present.
3.  **GooglePlacesStorageService**
    -   Saves new Google places.
    -   Updates existing records using `google_place_id`.
    -   Persists regular opening-hour periods.
    -   Keeps the main place record and its opening hours consistent
        within a transaction.
4.  **Repositories**
    -   Provide database access for the Google place and opening-hours
        tables.

## 4. Database schema

The initial schema contains two tables:

1.  `google_places` --- one row per Google Place ID.
2.  `google_place_opening_hours` --- one row per opening-hours period.

The schema supports missing Google fields through nullable columns.
Google Place ID is the external unique identifier used for
insert-or-update operations.

### 4.1 `google_places`

Stores the main details returned for each Google place.

  ---------------------------------------------------------------------------------------------
  Column                Type                              Nullable Description
  --------------------- -------------------- --------------------- ----------------------------
  `id`                  `BIGSERIAL`                             No Internal primary key

  `google_place_id`     `VARCHAR(255)`                          No Google Place ID; unique
                                                                   external identifier

  `display_name`        `VARCHAR(500)`                         Yes `displayName.text`

  `language_code`       `VARCHAR(20)`                          Yes `displayName.languageCode`

  `formatted_address`   `TEXT`                                 Yes `formattedAddress`

  `latitude`            `DOUBLE PRECISION`                      No `location.latitude`

  `longitude`           `DOUBLE PRECISION`                      No `location.longitude`

  `rating`              `NUMERIC(2,1)`                         Yes Google rating

  `user_rating_count`   `INTEGER`                              Yes `userRatingCount`

  `business_status`     `VARCHAR(50)`                          Yes `businessStatus`

  `restroom`            `BOOLEAN`                              Yes `restroom`, when returned

  `created_at`          `TIMESTAMPTZ`                           No Record creation timestamp

  `updated_at`          `TIMESTAMPTZ`                           No Last database update
                                                                   timestamp

  `fetched_at`          `TIMESTAMPTZ`                          Yes Time the Google data was
                                                                   fetched
  ---------------------------------------------------------------------------------------------

#### SQL

``` sql
CREATE TABLE google_places (
    id BIGSERIAL PRIMARY KEY,
    google_place_id VARCHAR(255) NOT NULL UNIQUE,

    display_name VARCHAR(500),
    language_code VARCHAR(20),
    formatted_address TEXT,

    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,

    rating NUMERIC(2,1),
    user_rating_count INTEGER,

    business_status VARCHAR(50),
    restroom BOOLEAN,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fetched_at TIMESTAMPTZ,

    CONSTRAINT chk_google_place_latitude
        CHECK (latitude BETWEEN -90 AND 90),

    CONSTRAINT chk_google_place_longitude
        CHECK (longitude BETWEEN -180 AND 180),

    CONSTRAINT chk_google_place_rating
        CHECK (rating IS NULL OR rating BETWEEN 0 AND 5)
);

CREATE INDEX idx_google_places_location
    ON google_places(latitude, longitude);

CREATE INDEX idx_google_places_display_name
    ON google_places(display_name);
```

### 4.2 `google_place_opening_hours`

Stores recurring weekly opening-hour periods. A place may have multiple
periods on the same day, and a period may close on the following day.

  ----------------------------------------------------------------------------------------------
  Column                Type                          Nullable Description
  --------------------- ---------------- --------------------- ---------------------------------
  `id`                  `BIGSERIAL`                         No Internal primary key

  `google_place_id`     `VARCHAR(255)`                      No Foreign key to
                                                               `google_places.google_place_id`

  `day_of_week`         `SMALLINT`                          No Google day number for opening day

  `period_order`        `SMALLINT`                          No Order of the period within that
                                                               day

  `open_time`           `TIME`                             Yes Opening time

  `close_day_of_week`   `SMALLINT`                         Yes Google day number for closing
                                                               day, when supplied

  `close_time`          `TIME`                             Yes Closing time; nullable for an
                                                               open-ended period

  `is_24_hours`         `BOOLEAN`                           No Indicates an all-day opening
                                                               period
  ----------------------------------------------------------------------------------------------

Google day numbering:

    Google value Day
  -------------- -----------
               0 Sunday
               1 Monday
               2 Tuesday
               3 Wednesday
               4 Thursday
               5 Friday
               6 Saturday

#### SQL

``` sql
CREATE TABLE google_place_opening_hours (
    id BIGSERIAL PRIMARY KEY,

    google_place_id VARCHAR(255) NOT NULL,
    day_of_week SMALLINT NOT NULL,
    period_order SMALLINT NOT NULL DEFAULT 1,

    open_time TIME,
    close_day_of_week SMALLINT,
    close_time TIME,

    is_24_hours BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_google_place_opening_hours
        FOREIGN KEY (google_place_id)
        REFERENCES google_places(google_place_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_opening_day
        CHECK (day_of_week BETWEEN 0 AND 6),

    CONSTRAINT chk_closing_day
        CHECK (
            close_day_of_week IS NULL
            OR close_day_of_week BETWEEN 0 AND 6
        ),

    CONSTRAINT uq_google_place_opening_period
        UNIQUE (google_place_id, day_of_week, period_order)
);

CREATE INDEX idx_google_place_opening_hours_place
    ON google_place_opening_hours(google_place_id);
```

### Opening-hours handling notes

-   Map `regularOpeningHours.periods` into `google_place_opening_hours`.
-   Preserve multiple periods for the same day using `period_order`.
-   Map the opening and closing day values using Google's day numbering.
-   Handle a missing closing time as an open-ended period where the API
    response indicates that format.
-   For a 24-hour schedule, set `is_24_hours` based on the returned
    period data.
-   Do not store `currentOpeningHours` in this recurring schedule table.
    It is date-specific and should not be treated as the regular weekly
    schedule.
-   `openNow` is a time-dependent value, not a permanent property of the
    place.

## 5. Google response-to-database mapping

  Google Places response field    Database destination
  ------------------------------- -----------------------------------
  `id`                            `google_places.google_place_id`
  `displayName.text`              `google_places.display_name`
  `displayName.languageCode`      `google_places.language_code`
  `formattedAddress`              `google_places.formatted_address`
  `location.latitude`             `google_places.latitude`
  `location.longitude`            `google_places.longitude`
  `rating`                        `google_places.rating`
  `userRatingCount`               `google_places.user_rating_count`
  `businessStatus`                `google_places.business_status`
  `restroom`                      `google_places.restroom`
  `regularOpeningHours.periods`   `google_place_opening_hours`
  Fetch timestamp                 `google_places.fetched_at`

Only fields actually returned by the API should be mapped. Missing
optional values should remain `NULL`; they should not be replaced with
assumed values.

## 6. Spring Boot application changes

### 6.1 Suggested package structure

``` text
com.example.petrolfinder
|
+-- controller
|   +-- GooglePlacesController.java
|
+-- service
|   +-- GooglePlacesService.java
|   +-- GooglePlacesStorageService.java
|
+-- repository
|   +-- GooglePlacesRepository.java
|   +-- GooglePlaceOpeningHoursRepository.java
|
+-- entity
|   +-- GooglePlace.java
|   +-- GooglePlaceOpeningHours.java
|
+-- dto
|   +-- GooglePlacesResponse.java
|   +-- GooglePlaceDto.java
|   +-- GoogleOpeningHoursDto.java
|
+-- mapper
    +-- GooglePlacesMapper.java
```

Package names can be adjusted to match the existing project structure.

### 6.2 Entity classes

Create JPA entities corresponding to:

-   `GooglePlace`
    -   Maps to `google_places`.
    -   Uses `google_place_id` as the unique external identifier.
    -   Has a one-to-many relationship with opening-hour periods.
-   `GooglePlaceOpeningHours`
    -   Maps to `google_place_opening_hours`.
    -   References the parent Google place using `google_place_id`.
    -   Stores one row per opening-hours period.

Use `BigDecimal` for the rating field, `Double` for latitude and
longitude, `LocalTime` for opening and closing times, and
`OffsetDateTime` or an equivalent timezone-aware Java type for
timestamps.

### 6.3 DTO and mapping changes

Create DTOs that reflect the Google response structure, including nested
objects such as:

-   `displayName`
-   `location`
-   `regularOpeningHours`
-   `periods`
-   `open`
-   `close`

The mapper should convert the nested API response into the database
entities. Keep API DTOs separate from JPA entities so changes in the
external response do not directly affect the persistence model.

### 6.4 Repository changes

Create:

-   `GooglePlacesRepository`
    -   Find a place by Google Place ID.
    -   Check whether a Google Place ID already exists.
    -   Save or update a place.
-   `GooglePlaceOpeningHoursRepository`
    -   Find opening-hour periods by Google Place ID.
    -   Delete and replace recurring periods when refreshing a place's
        schedule.

### 6.5 Storage service changes

Implement `GooglePlacesStorageService` with transactional persistence.

Recommended processing:

1.  Receive the validated and mapped place data.
2.  Find the existing record by `google_place_id`.
3.  Insert a new record if it does not exist.
4.  Update the existing record if it already exists.
5.  Refresh the recurring opening-hour periods when opening-hours data
    is supplied.
6.  Update `fetched_at` and `updated_at`.
7.  Commit the place and opening-hours changes together.

Use the Google Place ID as the upsert key. Do not use coordinates or
display name as a unique key.

If a later API response does not contain an optional field, avoid
replacing an existing value with `NULL` unless the update policy
explicitly says that the missing field means the value should be
cleared.

### 6.6 Controller changes

Add or update an endpoint to trigger the Google Places fetch-and-store
flow if the existing application requires an API endpoint for this
operation.

The controller should delegate to the service layer. It should not
contain Google response parsing or database persistence logic.

The exact endpoint path and request parameters should follow the
existing application's API conventions.

## 7. Insert and update behaviour

  -----------------------------------------------------------------------
  Scenario                            Expected behaviour
  ----------------------------------- -----------------------------------
  New Google Place ID                 Insert a new `google_places` row

  Existing Google Place ID            Update the existing row

  Same place returned more than once  Do not create duplicate rows

  Optional field missing              Store `NULL` for a new record;
                                      apply the agreed missing-field
                                      update policy for existing records

  Opening hours returned              Persist the returned regular
                                      periods

  Opening hours not returned          Do not assume a schedule

  Multiple periods in one day         Store each period separately

  API result is not a petrol station  Do not store it as a petrol-pump
                                      record
  -----------------------------------------------------------------------

## 8. Data integrity and implementation considerations

-   `google_place_id` must remain unique.
-   Latitude and longitude must pass range validation.
-   Rating must be between 0 and 5 when present.
-   Google response fields are optional unless explicitly required by
    the application.
-   Use database transactions when saving a place and its opening-hour
    periods.
-   Use parameterized persistence through Spring Data JPA.
-   Keep Google API credentials in application configuration or
    environment variables, not in source code.
-   Log ingestion failures without logging API keys.
-   Keep the public dataset separate from these Google tables.
-   Do not overwrite public-dataset coordinates or attributes as part of
    this Google ingestion change.

## 9. Out of scope

This document covers only Google Places data persistence and the
application changes needed for it.

The following are not part of this implementation:

-   Matching Google records against the public dataset.
-   Merging Google and public-dataset records.
-   Machine-learning model training or prediction.
-   Petrol-pump recommendation and ranking.
-   Analytics dashboards.
-   Historical rating and business-status tracking.

## 10. Implementation checklist

-   [x] Create `google_places`.
-   [x] Create `google_place_opening_hours`.
-   [x] Add JPA entities and relationships.
-   [x] Add Google Places response DTOs.
-   [x] Add response-to-entity mapper.
-   [x] Add Spring Data repositories.
-   [x] Implement transactional insert-or-update logic using Google
    Place ID.
-   [x] Implement recurring opening-hours persistence.
-   [x] Validate petrol-station results before storing them.
-   [x] Handle missing optional response fields.
-   [ ] Test new-place insertion.
-   [ ] Test existing-place updates without duplicates.
-   [ ] Test missing fields and multiple opening periods.
-   [ ] Verify the stored records against a sample Google Places API
    response.

## 11. Implementation notes (2026-11-04)

### Database

Supabase (PostgreSQL) is used as the database. The migration SQL is
compatible with PostgreSQL with two schema adjustments made during
the H2 prototype phase that are retained:

-   `BIGSERIAL` replaced with `BIGINT GENERATED ALWAYS AS IDENTITY`
    (equivalent in PostgreSQL 10+).
-   `NUMERIC(2,1)` widened to `NUMERIC(3,1)` to accommodate the full
    0–5 rating range without overflow.

The JDBC connection uses a direct connection to Supabase on port 5432.
Credentials are stored in `application.properties` and must not be
committed to source control.

### Spring Boot wiring

| Component | Location |
|---|---|
| Flyway migration | `src/main/resources/db/migration/V1__create_google_places_tables.sql` |
| `GooglePlace` entity | `model/GooglePlace.java` |
| `GooglePlaceOpeningHours` entity | `model/GooglePlaceOpeningHours.java` |
| `GooglePlacesRepository` | `repository/GooglePlacesRepository.java` |
| `GooglePlaceOpeningHoursRepository` | `repository/GooglePlaceOpeningHoursRepository.java` |
| `GooglePlacesMapper` | `mapper/GooglePlacesMapper.java` |
| `GooglePlacesStorageService` | `service/GooglePlacesStorageService.java` |
| `AsyncConfig` | `config/AsyncConfig.java` |

### Insert behaviour

Insert-only policy is in effect: if a `google_place_id` already exists
the record is skipped without any update. The upsert path described in
Section 7 is not implemented at this stage.

### Async execution

`GooglePlacesStorageService.saveNewPlacesAsync` is annotated with
`@Async("placesStorageExecutor")` and called from
`StationSearchService` immediately after the Google Places API response
is received. The search response is returned to the user without
waiting for the database write to complete.
