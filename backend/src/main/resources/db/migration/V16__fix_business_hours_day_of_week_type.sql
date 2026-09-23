-- V15 used TINYINT for day_of_week, but the BusinessHour entity maps a Java `int` to SQL
-- INTEGER by default - Hibernate's schema validation (ddl-auto: validate in prod) rejects the
-- mismatch and refuses to start. Table has never had a successful write (the app couldn't boot
-- since V15), so this is a safe, no-data-loss column type change.
ALTER TABLE business_hours MODIFY day_of_week INT NOT NULL;
