package com.exhibitorreg.organiser;

import com.exhibitorreg.admin.Event;
import com.exhibitorreg.admin.EventRepository;
import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.common.exception.ConflictException;
import com.exhibitorreg.common.exception.NotFoundException;
import com.exhibitorreg.organiser.dto.CreateLinkRequest;
import com.exhibitorreg.organiser.dto.LinkResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationLinkService {

    private final ExhibitorRegistrationLinkRepository linkRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final String publicFrontendBaseUrl;

    public RegistrationLinkService(
            ExhibitorRegistrationLinkRepository linkRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            @Value("${app.public-frontend-base-url}") String publicFrontendBaseUrl) {
        this.linkRepository = linkRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.publicFrontendBaseUrl = publicFrontendBaseUrl;
    }

    @Transactional
    public LinkResponse createLink(AuthenticatedPrincipal principal, CreateLinkRequest request) {
        Event activeEvent = eventRepository.findByActiveTrue()
                .orElseThrow(() -> new ConflictException("No active event is configured."));
        User organiser = userRepository.findById(principal.userId())
                .orElseThrow(() -> new NotFoundException("User not found: " + principal.userId()));

        ExhibitorRegistrationLink link = new ExhibitorRegistrationLink();
        link.setEvent(activeEvent);
        link.setCreatedBy(organiser);
        link.setExpiresAt(request.expiresAt());
        linkRepository.save(link);

        return toResponse(link);
    }

    @Transactional(readOnly = true)
    public List<LinkResponse> listLinks() {
        return linkRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public LinkResponse deactivate(UUID id) {
        ExhibitorRegistrationLink link = linkRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Registration link not found: " + id));
        link.setActive(false);
        linkRepository.save(link);
        return toResponse(link);
    }

    private LinkResponse toResponse(ExhibitorRegistrationLink link) {
        return new LinkResponse(
                link.getId(),
                publicFrontendBaseUrl + link.getId(),
                link.getExpiresAt(),
                link.isActive(),
                link.getCreatedAt());
    }
}
