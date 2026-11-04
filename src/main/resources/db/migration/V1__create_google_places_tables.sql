CREATE TABLE google_places (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    google_place_id     VARCHAR(255)            NOT NULL UNIQUE,

    display_name        VARCHAR(500),
    language_code       VARCHAR(20),
    formatted_address   TEXT,

    latitude            DOUBLE PRECISION        NOT NULL,
    longitude           DOUBLE PRECISION        NOT NULL,

    rating              NUMERIC(3,1),
    user_rating_count   INTEGER,

    business_status     VARCHAR(50),
    restroom            BOOLEAN,

    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fetched_at          TIMESTAMP WITH TIME ZONE,

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

CREATE TABLE google_place_opening_hours (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    google_place_id     VARCHAR(255)    NOT NULL,
    day_of_week         SMALLINT        NOT NULL,
    period_order        SMALLINT        NOT NULL DEFAULT 1,

    open_time           TIME,
    close_day_of_week   SMALLINT,
    close_time          TIME,

    is_24_hours         BOOLEAN         NOT NULL DEFAULT FALSE,

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
