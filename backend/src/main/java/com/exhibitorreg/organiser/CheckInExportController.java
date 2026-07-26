package com.exhibitorreg.organiser;

import com.exhibitorreg.validator.CheckInScan;
import com.exhibitorreg.validator.CheckInScanRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organiser/check-ins")
public class CheckInExportController {

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader("personName", "designation", "companyName", "eventDayDate", "validatorUsername", "scannedAt")
            .get();

    private final CheckInScanRepository checkInScanRepository;

    public CheckInExportController(CheckInScanRepository checkInScanRepository) {
        this.checkInScanRepository = checkInScanRepository;
    }

    @GetMapping("/export")
    public void export(@RequestParam UUID eventDayId, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"check-ins-" + eventDayId + ".csv\"");

        try (CSVPrinter printer = new CSVPrinter(response.getWriter(), CSV_FORMAT)) {
            for (CheckInScan scan : checkInScanRepository.findByEventDayId(eventDayId)) {
                printer.printRecord(
                        scan.getExhibitorPerson().getName(),
                        scan.getExhibitorPerson().getDesignation(),
                        scan.getExhibitorPerson().getCompany().getName(),
                        scan.getEventDay().getDate(),
                        scan.getValidator().getUsername(),
                        scan.getCreatedAt());
            }
        }
    }
}
