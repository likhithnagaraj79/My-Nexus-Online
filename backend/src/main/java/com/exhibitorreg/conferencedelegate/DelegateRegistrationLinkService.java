package com.exhibitorreg.conferencedelegate;

import com.exhibitorreg.admin.Event;
import com.exhibitorreg.admin.EventRepository;
import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.common.exception.ConflictException;
import com.exhibitorreg.common.exception.NotFoundException;
import com.exhibitorreg.conferencedelegate.dto.CreateDelegateLinkRequest;
import com.exhibitorreg.conferencedelegate.dto.DelegateLinkResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DelegateRegistrationLinkService {

    private final ConferenceDelegateRegistrationLinkRepository linkRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final String publicFrontendDelegateBaseUrl;

    public DelegateRegistrationLinkService(
            ConferenceDelegateRegistrationLinkRepository linkRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            @Value("${app.public-frontend-delegate-base-url}") String publicFrontendDelegateBaseUrl) {
        this.linkRepository = linkRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.publicFrontendDelegateBaseUrl = publicFrontendDelegateBaseUrl;
    }

    @Transactional
    public DelegateLinkResponse createLink(AuthenticatedPrincipal principal, CreateDelegateLinkRequest request) {
        Event activeEvent = eventRepository.findByActiveTrue()
                .orElseThrow(() -> new ConflictException("No active event is configured."));
        User organiser = userRepository.findById(principal.userId())
                .orElseThrow(() -> new NotFoundException("User not found: " + principal.userId()));

        ConferenceDelegateRegistrationLink link = new ConferenceDelegateRegistrationLink();
        link.setEvent(activeEvent);
        link.setCreatedBy(organiser);
        link.setExpiresAt(request.expiresAt());
        linkRepository.save(link);

        return toResponse(link);
    }

    @Transactional(readOnly = true)
    public List<DelegateLinkResponse> listLinks() {
        return linkRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public DelegateLinkResponse deactivate(UUID id) {
        ConferenceDelegateRegistrationLink link = linkRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Delegate registration link not found: " + id));
        link.setActive(false);
        linkRepository.save(link);
        return toResponse(link);
    }

    private DelegateLinkResponse toResponse(ConferenceDelegateRegistrationLink link) {
        return new DelegateLinkResponse(
                link.getId(),
                publicFrontendDelegateBaseUrl + link.getId(),
                link.getExpiresAt(),
                link.isActive(),
                link.getCreatedAt());
    }
}
