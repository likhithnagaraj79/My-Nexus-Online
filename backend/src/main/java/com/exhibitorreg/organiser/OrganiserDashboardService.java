package com.exhibitorreg.organiser;

import com.exhibitorreg.crew.labourpass.LabourPassRepository;
import com.exhibitorreg.crew.labourpass.LabourPassType;
import com.exhibitorreg.crew.labourpass.dto.LabourPassSummary;
import com.exhibitorreg.organiser.dto.AnalyticsResponse;
import com.exhibitorreg.organiser.dto.DashboardResponse;
import com.exhibitorreg.publicregistration.ExhibitorPersonRepository;
import com.exhibitorreg.publicregistration.ExhibitorSubmissionRepository;
import com.exhibitorreg.validator.CheckInScanRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganiserDashboardService {

    private final ExhibitorPersonRepository exhibitorPersonRepository;
    private final ExhibitorSubmissionRepository exhibitorSubmissionRepository;
    private final CheckInScanRepository checkInScanRepository;
    private final LabourPassRepository labourPassRepository;

    public OrganiserDashboardService(
            ExhibitorPersonRepository exhibitorPersonRepository,
            ExhibitorSubmissionRepository exhibitorSubmissionRepository,
            CheckInScanRepository checkInScanRepository,
            LabourPassRepository labourPassRepository) {
        this.exhibitorPersonRepository = exhibitorPersonRepository;
        this.exhibitorSubmissionRepository = exhibitorSubmissionRepository;
        this.checkInScanRepository = checkInScanRepository;
        this.labourPassRepository = labourPassRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        return new DashboardResponse(
                exhibitorPersonRepository.countByPrintedTrue(),
                exhibitorPersonRepository.countByIssuedTrue(),
                exhibitorSubmissionRepository.count(),
                exhibitorPersonRepository.count(),
                checkInScanRepository.count());
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse analytics() {
        return new AnalyticsResponse(
                checkInScanRepository.count(),
                labourPassRepository.countByPassType(LabourPassType.VENDOR),
                labourPassRepository.countByPassType(LabourPassType.EXHIBITOR),
                labourPassRepository.countByPassType(LabourPassType.FABRICATOR_LABOUR),
                exhibitorPersonRepository.countByPrintedTrue());
    }

    @Transactional(readOnly = true)
    public List<LabourPassSummary> listLabourPasses() {
        return labourPassRepository.findAll().stream().map(LabourPassSummary::from).toList();
    }
}
