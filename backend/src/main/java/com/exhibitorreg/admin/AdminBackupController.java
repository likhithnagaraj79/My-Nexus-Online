package com.exhibitorreg.admin;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/backup")
public class AdminBackupController {

    private final AdminBackupService adminBackupService;

    public AdminBackupController(AdminBackupService adminBackupService) {
        this.adminBackupService = adminBackupService;
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"exhibitor-registration-backup-" + Instant.now().toEpochMilli() + ".dump\"");
        adminBackupService.exportBackup(response.getOutputStream());
    }

    @PostMapping(value = "/restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void restore(
            @RequestParam("file") MultipartFile file, @RequestParam("confirm") boolean confirm) throws IOException {
        adminBackupService.restoreBackup(file, confirm);
    }
}
