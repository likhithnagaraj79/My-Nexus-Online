package com.exhibitorreg.crew.exhibitorpass;

import com.exhibitorreg.auth.AuthenticatedPrincipal;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.auth.UserRepository;
import com.exhibitorreg.common.exception.BusinessRuleViolationException;
import com.exhibitorreg.common.exception.NotFoundException;
import com.exhibitorreg.crew.exhibitorpass.dto.ExhibitorPassSummary;
import com.exhibitorreg.crew.exhibitorpass.dto.IssueRequest;
import com.exhibitorreg.crew.exhibitorpass.dto.PrintRequest;
import com.exhibitorreg.publicregistration.ExhibitorPerson;
import com.exhibitorreg.publicregistration.ExhibitorPersonRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operates directly on {@link ExhibitorPerson} (owned by the publicregistration package) —
 * this feature has no entity of its own. ExhibitorPerson is a genuinely shared aggregate:
 * public creates it, this service mutates printed/issued status, validator reads it to
 * resolve scanned QR codes.
 */
@Service
public class ExhibitorPassService {

    private static final int QR_SIZE_PX = 300;

    private final ExhibitorPersonRepository exhibitorPersonRepository;
    private final UserRepository userRepository;

    public ExhibitorPassService(ExhibitorPersonRepository exhibitorPersonRepository, UserRepository userRepository) {
        this.exhibitorPersonRepository = exhibitorPersonRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ExhibitorPassSummary> list(UUID companyId, Boolean printed, Boolean issued, String q) {
        Specification<ExhibitorPerson> spec = Specification.unrestricted();

        if (companyId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("company").get("id"), companyId));
        }
        if (printed != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("printed"), printed));
        }
        if (issued != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("issued"), issued));
        }
        if (q != null && !q.isBlank()) {
            String likePattern = "%" + q.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), likePattern),
                    cb.like(cb.lower(root.get("designation")), likePattern)));
        }

        return exhibitorPersonRepository.findAll(spec).stream().map(ExhibitorPassSummary::from).toList();
    }

    @Transactional
    public List<ExhibitorPassSummary> print(AuthenticatedPrincipal principal, PrintRequest request) {
        List<ExhibitorPerson> targets = resolvePrintTargets(request);
        User crewMember = getUserOrThrow(principal.userId());

        Instant now = Instant.now();
        for (ExhibitorPerson person : targets) {
            person.setPrinted(true);
            person.setPrintedAt(now);
            person.setPrintedBy(crewMember);
        }
        exhibitorPersonRepository.saveAll(targets);

        return targets.stream().map(ExhibitorPassSummary::from).toList();
    }

    @Transactional
    public ExhibitorPassSummary issue(AuthenticatedPrincipal principal, UUID personId, IssueRequest request) {
        ExhibitorPerson person = getPersonOrThrow(personId);
        User crewMember = getUserOrThrow(principal.userId());

        person.setIssued(true);
        person.setIssuedAt(Instant.now());
        person.setIssuedBy(crewMember);
        person.setIssuedPhoneNumber(request.phoneNumber());
        exhibitorPersonRepository.save(person);

        return ExhibitorPassSummary.from(person);
    }

    @Transactional(readOnly = true)
    public byte[] generateQrPng(UUID personId) {
        ExhibitorPerson person = getPersonOrThrow(personId);
        try {
            BitMatrix matrix = new QRCodeWriter()
                    .encode(person.getId().toString(), BarcodeFormat.QR_CODE, QR_SIZE_PX, QR_SIZE_PX);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to generate badge QR code.", e);
        }
    }

    private List<ExhibitorPerson> resolvePrintTargets(PrintRequest request) {
        boolean hasPersonIds = request.personIds() != null && !request.personIds().isEmpty();
        boolean hasCompanyId = request.companyId() != null;

        if (hasPersonIds == hasCompanyId) {
            throw new BusinessRuleViolationException("Provide exactly one of personIds or companyId.");
        }

        return hasCompanyId
                ? exhibitorPersonRepository.findByCompanyId(request.companyId())
                : exhibitorPersonRepository.findAllById(request.personIds());
    }

    private ExhibitorPerson getPersonOrThrow(UUID id) {
        return exhibitorPersonRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Exhibitor person not found: " + id));
    }

    private User getUserOrThrow(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found: " + id));
    }
}
