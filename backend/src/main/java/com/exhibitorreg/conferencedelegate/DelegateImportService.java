package com.exhibitorreg.conferencedelegate;

import com.exhibitorreg.common.exception.BusinessRuleViolationException;
import com.exhibitorreg.conferencedelegate.dto.DelegateImportRowError;
import com.exhibitorreg.conferencedelegate.dto.DelegateImportSummary;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Bulk-imports Conference Delegates from a CSV uploaded by Organiser — bypasses the public
 * registration link entirely (imported delegates have {@code link=null}), same target table
 * {@code conference_delegates} the public form and Crew's print flow already use. */
@Service
public class DelegateImportService {

    private static final List<String> REQUIRED_COLUMNS =
            List.of("Name", "Company Name", "Designation", "Mobile Number", "Email");

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final CSVFormat TEMPLATE_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader(REQUIRED_COLUMNS.toArray(new String[0]))
            .get();

    private final ConferenceDelegateRepository delegateRepository;

    public DelegateImportService(ConferenceDelegateRepository delegateRepository) {
        this.delegateRepository = delegateRepository;
    }

    public void downloadTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"conference-delegates-template.csv\"");
        try (CSVPrinter printer = new CSVPrinter(response.getWriter(), TEMPLATE_FORMAT)) {
            printer.printRecord("Jane Doe", "Acme Corp", "Manager", "9876543210", "jane.doe@example.com");
        }
    }

    @Transactional
    public DelegateImportSummary importCsv(MultipartFile file) {
        List<DelegateImportRowError> errors = new ArrayList<>();
        List<ConferenceDelegate> toSave = new ArrayList<>();

        try (var reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setTrim(true)
                        .get()
                        .parse(reader)) {

            List<String> headerNames = parser.getHeaderNames();
            for (String required : REQUIRED_COLUMNS) {
                if (!headerNames.contains(required)) {
                    throw new BusinessRuleViolationException(
                            "CSV is missing required column: " + required
                                    + ". Expected columns: " + String.join(", ", REQUIRED_COLUMNS));
                }
            }

            for (CSVRecord record : parser) {
                // getRecordNumber() counts data records only (header is skipped from the count,
                // not just the iteration) — +1 maps back to the actual file line number a user
                // would see opening the CSV in a spreadsheet (header = line 1, first data = 2).
                int rowNumber = (int) record.getRecordNumber() + 1;
                String reason = validateRow(record);
                if (reason != null) {
                    errors.add(new DelegateImportRowError(rowNumber, reason));
                    continue;
                }

                ConferenceDelegate delegate = new ConferenceDelegate();
                String name = record.get("Name").trim();
                delegate.setName(name.isEmpty() ? null : name);
                delegate.setCompanyName(record.get("Company Name").trim());
                delegate.setDesignation(record.get("Designation").trim());
                delegate.setMobileNumber(record.get("Mobile Number").trim());
                delegate.setEmail(record.get("Email").trim());
                toSave.add(delegate);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read the uploaded CSV file.", e);
        }

        delegateRepository.saveAll(toSave);
        return new DelegateImportSummary(toSave.size(), errors);
    }

    private String validateRow(CSVRecord record) {
        String name = record.get("Name");
        String companyName = record.get("Company Name");
        String designation = record.get("Designation");
        String mobileNumber = record.get("Mobile Number");
        String email = record.get("Email");

        // Name is intentionally optional here — a blank name no longer rejects the row; Crew
        // fills it in later via the edit action. Only enforce the length limit when present.
        if (name != null && name.length() > 150) {
            return "Name must be at most 150 characters.";
        }
        if (companyName == null || companyName.isBlank()) {
            return "Company Name is required.";
        }
        if (companyName.length() > 200) {
            return "Company Name must be at most 200 characters.";
        }
        if (designation == null || designation.isBlank()) {
            return "Designation is required.";
        }
        if (designation.length() > 150) {
            return "Designation must be at most 150 characters.";
        }
        if (mobileNumber == null || mobileNumber.isBlank()) {
            return "Mobile Number is required.";
        }
        if (mobileNumber.length() > 15) {
            return "Mobile Number must be at most 15 characters.";
        }
        if (email == null || email.isBlank()) {
            return "Email is required.";
        }
        if (email.length() > 255 || !EMAIL_PATTERN.matcher(email).matches()) {
            return "Email is not a valid email address.";
        }
        return null;
    }
}
