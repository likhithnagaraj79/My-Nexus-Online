package com.exhibitorreg.crew.labourpass;

import com.exhibitorreg.admin.Event;
import com.exhibitorreg.admin.EventRepository;
import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.common.exception.BusinessRuleViolationException;
import com.exhibitorreg.common.exception.ConflictException;
import com.exhibitorreg.common.exception.NotFoundException;
import com.exhibitorreg.crew.labourpass.dto.CreateLabourPassRequest;
import com.exhibitorreg.crew.labourpass.dto.LabourPassSummary;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LabourPassService {

    private final LabourPassRepository labourPassRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public LabourPassService(
            LabourPassRepository labourPassRepository, EventRepository eventRepository, UserRepository userRepository) {
        this.labourPassRepository = labourPassRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public LabourPassSummary create(AuthenticatedPrincipal principal, CreateLabourPassRequest request) {
        // Defense-in-depth: the same rule is declaratively enforced on the request DTO via @AssertTrue.
        boolean stallNumberProvided = request.stallNumber() != null && !request.stallNumber().isBlank();
        if (request.passType() != LabourPassType.VENDOR && !stallNumberProvided) {
            throw new BusinessRuleViolationException(
                    "stallNumber is required for EXHIBITOR and FABRICATOR_LABOUR pass types.");
        }

        Event activeEvent = eventRepository.findByActiveTrue()
                .orElseThrow(() -> new ConflictException("No active event is configured."));
        User crewMember = userRepository.findById(principal.userId())
                .orElseThrow(() -> new NotFoundException("User not found: " + principal.userId()));

        LabourPass pass = new LabourPass();
        pass.setPassType(request.passType());
        pass.setPassCount(request.passCount());
        pass.setPhoneNumber(request.phoneNumber());
        pass.setStallNumber(request.stallNumber());
        pass.setEvent(activeEvent);
        pass.setIssuedBy(crewMember);
        labourPassRepository.save(pass);

        return LabourPassSummary.from(pass);
    }

    @Transactional(readOnly = true)
    public List<LabourPassSummary> listByEvent(UUID eventId) {
        return labourPassRepository.findByEventId(eventId).stream().map(LabourPassSummary::from).toList();
    }
}
