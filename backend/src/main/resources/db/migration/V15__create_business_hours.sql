-- Weekly opening hours for BUSINESS/COMPANY profiles (individual profiles don't have hours).
-- day_of_week matches java.time.DayOfWeek.getValue() (1=MONDAY .. 7=SUNDAY) so the backend
-- can use java.time directly with no translation table. One row per day is upserted from the
-- editor's 7-row form; a day with is_closed=TRUE has null opens_at/closes_at.

CREATE TABLE business_hours (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_profile_id  BIGINT      NOT NULL,
    day_of_week         TINYINT     NOT NULL,
    opens_at            TIME        NULL,
    closes_at           TIME        NULL,
    is_closed           BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_business_hours_profile_day UNIQUE (company_profile_id, day_of_week),
    CONSTRAINT chk_business_hours_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT fk_business_hours_profile FOREIGN KEY (company_profile_id) REFERENCES company_profiles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
