package com.exhibitorreg.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * Opt-in: exercises {@link AdminBackupService} against a REAL Postgres database (shelling out to
 * the real {@code pg_dump}/{@code pg_restore} binaries via {@link RealProcessRunner}), which the
 * H2-based integration tests elsewhere in this codebase deliberately can't cover. Not part of the
 * default {@code mvn test} run — gated behind {@code RUN_PG_BACKUP_IT=true} so environments
 * without the Postgres client tools on PATH aren't broken by it.
 *
 * <p>Targets a dedicated throwaway database, {@code exhibitor_registration_backup_it} — never
 * the real dev database or the separate {@code exhibitor_registration_e2e} database used by the
 * Playwright suite. One-time setup (the {@code exhibitor_app} role has no CREATEDB grant, so this
 * needs to be run once by a superuser): {@code createdb exhibitor_registration_backup_it -O
 * exhibitor_app}. See the README for the full opt-in test setup.
 *
 * <p>Credentials are read from the same {@code DB_USERNAME}/{@code DB_PASSWORD} env vars the app
 * itself uses locally — deliberately not hardcoded here to avoid committing a real secret.
 */
@EnabledIfEnvironmentVariable(named = "RUN_PG_BACKUP_IT", matches = "true")
class BackupRestoreRealPostgresIntegrationTest {

    private static final String DB_NAME = "exhibitor_registration_backup_it";
    private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/" + DB_NAME;

    private final String dbUsername = envOrDefault("DB_USERNAME", "exhibitor_app");
    private final String dbPassword = requiredEnv("DB_PASSWORD");

    private AdminBackupService service;

    @BeforeEach
    void resetSchema() {
        Flyway flyway = Flyway.configure()
                .dataSource(JDBC_URL, dbUsername, dbPassword)
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();

        service = new AdminBackupService(new RealProcessRunner(), JDBC_URL, dbUsername, dbPassword);
    }

    @Test
    void exportedBackupRestoresTheOriginalDataOverDifferentlySeededData() throws Exception {
        String originalEventName = "Backup-IT Original Expo " + UUID.randomUUID();
        seedEvent(originalEventName);

        ByteArrayOutputStream dumpOut = new ByteArrayOutputStream();
        service.exportBackup(dumpOut);
        byte[] dump = dumpOut.toByteArray();

        assertThat(dump).isNotEmpty();
        assertThat(new String(dump, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("PGDMP");

        // Simulate the DB having moved on since the backup was taken.
        String differentEventName = "Should Be Wiped By Restore " + UUID.randomUUID();
        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM events");
        }
        seedEvent(differentEventName);
        assertThat(eventNames()).containsExactly(differentEventName);

        MultipartFile restoreFile =
                new MockMultipartFile("file", "backup.dump", "application/octet-stream", dump);
        service.restoreBackup(restoreFile, true);

        assertThat(eventNames()).containsExactly(originalEventName);
    }

    private void seedEvent(String name) throws Exception {
        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO events (id, name, start_date, end_date, active, created_at, updated_at) "
                    + "VALUES ('" + UUID.randomUUID() + "', '" + name.replace("'", "''") + "', "
                    + "'2026-08-01', '2026-08-03', false, now(), now())");
        }
    }

    private java.util.List<String> eventNames() throws Exception {
        java.util.List<String> names = new java.util.ArrayList<>();
        try (Connection connection = connect();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT name FROM events")) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        }
        return names;
    }

    private Connection connect() throws Exception {
        return DriverManager.getConnection(JDBC_URL, dbUsername, dbPassword);
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "This opt-in test requires the " + name + " environment variable to be set.");
        }
        return value;
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
