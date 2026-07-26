package com.exhibitorreg.admin;

import com.exhibitorreg.common.exception.BusinessRuleViolationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Shells out to the local pg_dump/pg_restore binaries (already on PATH via the project's
 * "brew install postgresql" no-Docker setup) to back up and restore the whole database.
 */
@Service
public class AdminBackupService {

    private static final Logger log = LoggerFactory.getLogger(AdminBackupService.class);
    private static final byte[] PG_CUSTOM_FORMAT_MAGIC = "PGDMP".getBytes(StandardCharsets.US_ASCII);

    private final ProcessRunner processRunner;
    private final String jdbcUrl;
    private final String dbUsername;
    private final String dbPassword;

    public AdminBackupService(
            ProcessRunner processRunner,
            @Value("${spring.datasource.url}") String jdbcUrl,
            @Value("${spring.datasource.username}") String dbUsername,
            @Value("${spring.datasource.password}") String dbPassword) {
        this.processRunner = processRunner;
        this.jdbcUrl = jdbcUrl;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    public void exportBackup(OutputStream destination) throws IOException {
        DbConnectionInfo connectionInfo = DbConnectionInfo.parse(jdbcUrl);
        List<String> command = List.of(
                "pg_dump", "-Fc",
                "-h", connectionInfo.host(),
                "-p", String.valueOf(connectionInfo.port()),
                "-U", dbUsername,
                connectionInfo.database());

        Process process = processRunner.start(command, Map.of("PGPASSWORD", dbPassword));
        try (InputStream stdout = process.getInputStream()) {
            stdout.transferTo(destination);
        }
        awaitSuccess(process, "pg_dump");
    }

    public void restoreBackup(MultipartFile file, boolean confirm) throws IOException {
        if (!confirm) {
            throw new BusinessRuleViolationException(
                    "Restoring the database is destructive. Set confirm=true to proceed.");
        }

        Path tempFile = Files.createTempFile("exhibitor-restore-", ".dump");
        try {
            file.transferTo(tempFile);
            validatePgCustomFormat(tempFile);

            DbConnectionInfo connectionInfo = DbConnectionInfo.parse(jdbcUrl);
            List<String> command = List.of(
                    "pg_restore", "--clean", "--if-exists",
                    "-h", connectionInfo.host(),
                    "-p", String.valueOf(connectionInfo.port()),
                    "-U", dbUsername,
                    "-d", connectionInfo.database(),
                    tempFile.toString());

            Process process = processRunner.start(command, Map.of("PGPASSWORD", dbPassword));
            awaitSuccess(process, "pg_restore");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static void validatePgCustomFormat(Path file) throws IOException {
        byte[] header;
        try (InputStream in = Files.newInputStream(file)) {
            header = in.readNBytes(PG_CUSTOM_FORMAT_MAGIC.length);
        }
        if (!java.util.Arrays.equals(header, PG_CUSTOM_FORMAT_MAGIC)) {
            throw new BusinessRuleViolationException(
                    "Uploaded file is not a valid pg_dump custom-format archive.");
        }
    }

    private static void awaitSuccess(Process process, String toolName) {
        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(toolName + " was interrupted.", e);
        }
        if (exitCode != 0) {
            log.error("{} exited with status {}", toolName, exitCode);
            throw new IllegalStateException(toolName + " failed with exit code " + exitCode + ". Check server logs.");
        }
    }

    record DbConnectionInfo(String host, int port, String database) {
        static DbConnectionInfo parse(String jdbcUrl) {
            URI uri = URI.create(jdbcUrl.substring("jdbc:".length()));
            String path = uri.getPath();
            String database = path.startsWith("/") ? path.substring(1) : path;
            return new DbConnectionInfo(uri.getHost(), uri.getPort(), database);
        }
    }
}
