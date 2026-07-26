package com.exhibitorreg.validator;

import com.exhibitorreg.admin.EventDay;
import com.exhibitorreg.admin.EventDayRepository;
import com.exhibitorreg.admin.dto.EventDayResponse;
import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.common.exception.NotFoundException;
import com.exhibitorreg.publicregistration.ExhibitorPerson;
import com.exhibitorreg.publicregistration.ExhibitorPersonRepository;
import com.exhibitorreg.validator.dto.ScanRequest;
import com.exhibitorreg.validator.dto.ScanResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScanService {

    private final CheckInScanRepository checkInScanRepository;
    private final ExhibitorPersonRepository exhibitorPersonRepository;
    private final EventDayRepository eventDayRepository;
    private final UserRepository userRepository;

    public ScanService(
            CheckInScanRepository checkInScanRepository,
            ExhibitorPersonRepository exhibitorPersonRepository,
            EventDayRepository eventDayRepository,
            UserRepository userRepository) {
        this.checkInScanRepository = checkInScanRepository;
        this.exhibitorPersonRepository = exhibitorPersonRepository;
        this.eventDayRepository = eventDayRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<EventDayResponse> listEventDays(UUID eventId) {
        return eventDayRepository.findByEventIdOrderByDayNumber(eventId).stream()
                .map(EventDayResponse::from)
                .toList();
    }

    @Transactional
    public ScanResponse scan(AuthenticatedPrincipal principal, ScanRequest request) {
        // Throws IllegalArgumentException (-> 400) if the payload isn't a UUID at all.
        UUID personId = UUID.fromString(request.qrPayload());

        ExhibitorPerson person = exhibitorPersonRepository.findById(personId)
                .orElseThrow(() -> new NotFoundException("No exhibitor badge found for the scanned code."));
        EventDay eventDay = eventDayRepository.findById(request.eventDayId())
                .orElseThrow(() -> new NotFoundException("Event day not found: " + request.eventDayId()));
        User validator = userRepository.findById(principal.userId())
                .orElseThrow(() -> new NotFoundException("User not found: " + principal.userId()));

        // Recorded regardless — repeat scans are allowed (not hard-blocked) so scanner
        // glitches/double-taps never lock out a legitimate exhibitor; the response just flags it.
        boolean alreadyCheckedInToday =
                checkInScanRepository.existsByExhibitorPersonIdAndEventDayId(personId, eventDay.getId());

        CheckInScan scan = new CheckInScan();
        scan.setExhibitorPerson(person);
        scan.setEventDay(eventDay);
        scan.setValidator(validator);
        scan.setQrPayload(request.qrPayload());
        checkInScanRepository.save(scan);

        return new ScanResponse(
                scan.getId(),
                person.getId(),
                person.getName(),
                person.getCompany().getName(),
                alreadyCheckedInToday,
                scan.getCreatedAt());
    }
}
