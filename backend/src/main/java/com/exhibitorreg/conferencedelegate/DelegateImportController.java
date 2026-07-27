package com.exhibitorreg.conferencedelegate;

import com.exhibitorreg.conferencedelegate.dto.DelegateImportSummary;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/organiser/conference-delegates")
public class DelegateImportController {

    private final DelegateImportService delegateImportService;

    public DelegateImportController(DelegateImportService delegateImportService) {
        this.delegateImportService = delegateImportService;
    }

    @GetMapping("/import-template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        delegateImportService.downloadTemplate(response);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DelegateImportSummary importCsv(@RequestParam("file") MultipartFile file) {
        return delegateImportService.importCsv(file);
    }
}
