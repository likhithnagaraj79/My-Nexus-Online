package com.exhibitorreg.organiser;

import com.exhibitorreg.crew.labourpass.dto.LabourPassSummary;
import com.exhibitorreg.organiser.dto.AnalyticsResponse;
import com.exhibitorreg.organiser.dto.DashboardResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organiser")
public class OrganiserDashboardController {

    private final OrganiserDashboardService organiserDashboardService;

    public OrganiserDashboardController(OrganiserDashboardService organiserDashboardService) {
        this.organiserDashboardService = organiserDashboardService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return organiserDashboardService.dashboard();
    }

    @GetMapping("/analytics")
    public AnalyticsResponse analytics() {
        return organiserDashboardService.analytics();
    }

    /** Visible to Organiser and Admin, per the spec — URL rule for this path is `authenticated()`
     * in SecurityConfig; the actual role restriction is enforced here. */
    @GetMapping("/labour-passes")
    @PreAuthorize("hasAnyRole('ORGANISER','ADMIN')")
    public List<LabourPassSummary> labourPasses() {
        return organiserDashboardService.listLabourPasses();
    }
}
