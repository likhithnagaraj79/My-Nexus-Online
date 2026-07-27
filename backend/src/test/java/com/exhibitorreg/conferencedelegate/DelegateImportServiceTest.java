package com.exhibitorreg.conferencedelegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import com.exhibitorreg.common.exception.BusinessRuleViolationException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class DelegateImportServiceTest {

    @Mock
    private ConferenceDelegateRepository delegateRepository;

    private DelegateImportService service;

    @BeforeEach
    void setUp() {
        service = new DelegateImportService(delegateRepository);
    }

    private static MockMultipartFile csvFile(String content) {
        return new MockMultipartFile("file", "delegates.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @SuppressWarnings("unchecked")
    void importsAllValidRows() {
        String csv = "Name,Company Name,Designation,Mobile Number,Email\n"
                + "Alice,Acme Corp,Manager,9876543210,alice@example.com\n"
                + "Bob,Acme Corp,Engineer,9876543211,bob@example.com\n";

        var summary = service.importCsv(csvFile(csv));

        assertThat(summary.importedCount()).isEqualTo(2);
        assertThat(summary.errors()).isEmpty();

        ArgumentCaptor<java.util.List<ConferenceDelegate>> captor = ArgumentCaptor.forClass(java.util.List.class);
        verify(delegateRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(ConferenceDelegate::getName).containsExactly("Alice", "Bob");
        assertThat(captor.getValue()).allSatisfy(d -> assertThat(d.getLink()).isNull());
    }

    @Test
    void reportsInvalidRowsButStillImportsValidOnes() {
        String csv = "Name,Company Name,Designation,Mobile Number,Email\n"
                + "Alice,Acme Corp,Manager,9876543210,alice@example.com\n"
                + ",Acme Corp,Engineer,9876543211,bob@example.com\n"
                + "Carol,Acme Corp,Lead,9876543212,not-an-email\n";

        var summary = service.importCsv(csvFile(csv));

        assertThat(summary.importedCount()).isEqualTo(1);
        assertThat(summary.errors()).hasSize(2);
        assertThat(summary.errors().get(0).rowNumber()).isEqualTo(3);
        assertThat(summary.errors().get(0).reason()).contains("Name is required");
        assertThat(summary.errors().get(1).rowNumber()).isEqualTo(4);
        assertThat(summary.errors().get(1).reason()).contains("valid email");
    }

    @Test
    void rejectsCsvMissingARequiredColumn() {
        String csv = "Name,Company Name,Designation,Mobile Number\n" + "Alice,Acme Corp,Manager,9876543210\n";

        assertThatThrownBy(() -> service.importCsv(csvFile(csv))).isInstanceOf(BusinessRuleViolationException.class);
    }
}
