package com.exhibitorreg.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.exhibitorreg.common.exception.BusinessRuleViolationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class AdminBackupServiceTest {

    @Mock
    private ProcessRunner processRunner;

    @Mock
    private Process process;

    private AdminBackupService service;

    @BeforeEach
    void setUp() {
        service = new AdminBackupService(
                processRunner,
                "jdbc:postgresql://localhost:5432/exhibitor_registration_dev",
                "exhibitor_app",
                "secret");
    }

    @Test
    void connectionInfoParsesHostPortAndDatabaseFromJdbcUrl() {
        var info = AdminBackupService.DbConnectionInfo.parse(
                "jdbc:postgresql://db.internal:5433/exhibitor_registration_dev");

        assertThat(info.host()).isEqualTo("db.internal");
        assertThat(info.port()).isEqualTo(5433);
        assertThat(info.database()).isEqualTo("exhibitor_registration_dev");
    }

    @Test
    void exportStreamsStdoutOnSuccess() throws Exception {
        byte[] dumpBytes = "fake-dump-content".getBytes();
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(dumpBytes));
        when(process.waitFor()).thenReturn(0);
        when(processRunner.start(any(), any())).thenReturn(process);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.exportBackup(out);

        assertThat(out.toByteArray()).isEqualTo(dumpBytes);
    }

    @Test
    void exportThrowsWhenPgDumpExitsNonZero() throws Exception {
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.waitFor()).thenReturn(1);
        when(processRunner.start(any(), any())).thenReturn(process);

        assertThatThrownBy(() -> service.exportBackup(new ByteArrayOutputStream()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void restoreWithoutConfirmationIsRejectedBeforeTouchingProcessRunner() {
        MultipartFile file = new MockMultipartFile(
                "file", "backup.dump", "application/octet-stream", "PGDMPrestofthecontent".getBytes());

        assertThatThrownBy(() -> service.restoreBackup(file, false))
                .isInstanceOf(BusinessRuleViolationException.class);
        verifyNoInteractions(processRunner);
    }

    @Test
    void restoreWithInvalidMagicBytesIsRejected() {
        MultipartFile file = new MockMultipartFile(
                "file", "backup.dump", "application/octet-stream", "NOT-A-VALID-DUMP-FILE".getBytes());

        assertThatThrownBy(() -> service.restoreBackup(file, true))
                .isInstanceOf(BusinessRuleViolationException.class);
        verifyNoInteractions(processRunner);
    }

    @Test
    void restoreWithValidHeaderAndConfirmationInvokesPgRestore() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "file", "backup.dump", "application/octet-stream", "PGDMPrestofthecontent".getBytes());
        when(process.waitFor()).thenReturn(0);
        when(processRunner.start(any(), any())).thenReturn(process);

        service.restoreBackup(file, true);

        verify(processRunner).start(any(), any());
    }

    @Test
    void restoreThrowsWhenPgRestoreExitsNonZero() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "file", "backup.dump", "application/octet-stream", "PGDMPrestofthecontent".getBytes());
        when(process.waitFor()).thenReturn(1);
        when(processRunner.start(any(), any())).thenReturn(process);

        assertThatThrownBy(() -> service.restoreBackup(file, true)).isInstanceOf(IllegalStateException.class);
    }
}
