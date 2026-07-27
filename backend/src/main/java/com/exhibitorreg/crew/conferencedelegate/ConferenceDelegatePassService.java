package com.exhibitorreg.crew.conferencedelegate;

import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.common.exception.NotFoundException;
import com.exhibitorreg.conferencedelegate.ConferenceDelegate;
import com.exhibitorreg.conferencedelegate.ConferenceDelegateRepository;
import com.exhibitorreg.crew.conferencedelegate.dto.DelegatePassSummary;
import com.exhibitorreg.crew.conferencedelegate.dto.PrintDelegatesRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operates directly on {@link ConferenceDelegate} (owned by the conferencedelegate package) —
 * mirrors {@code ExhibitorPassService} but simpler: no issue tracking, no QR/check-in. */
@Service
public class ConferenceDelegatePassService {

    private final ConferenceDelegateRepository delegateRepository;
    private final UserRepository userRepository;

    public ConferenceDelegatePassService(ConferenceDelegateRepository delegateRepository, UserRepository userRepository) {
        this.delegateRepository = delegateRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<DelegatePassSummary> list(Boolean printed, String q) {
        Specification<ConferenceDelegate> spec = Specification.unrestricted();

        if (printed != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("printed"), printed));
        }
        if (q != null && !q.isBlank()) {
            String likePattern = "%" + q.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), likePattern),
                    cb.like(cb.lower(root.get("designation")), likePattern),
                    cb.like(cb.lower(root.get("companyName")), likePattern)));
        }

        return delegateRepository.findAll(spec).stream().map(DelegatePassSummary::from).toList();
    }

    @Transactional
    public List<DelegatePassSummary> print(AuthenticatedPrincipal principal, PrintDelegatesRequest request) {
        List<ConferenceDelegate> targets = delegateRepository.findAllById(request.personIds());
        User crewMember = userRepository.findById(principal.userId())
                .orElseThrow(() -> new NotFoundException("User not found: " + principal.userId()));

        Instant now = Instant.now();
        for (ConferenceDelegate delegate : targets) {
            delegate.setPrinted(true);
            delegate.setPrintedAt(now);
            delegate.setPrintedBy(crewMember);
        }
        delegateRepository.saveAll(targets);

        return targets.stream().map(DelegatePassSummary::from).toList();
    }
}
