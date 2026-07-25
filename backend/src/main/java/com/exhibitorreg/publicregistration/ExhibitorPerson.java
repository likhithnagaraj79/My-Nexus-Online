package com.exhibitorreg.publicregistration;

import com.exhibitorreg.auth.User;
import com.exhibitorreg.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single registered person under a company's submission. This row's own {@code id}
 * (inherited UUID PK) is the payload embedded in that person's printed badge QR code.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exhibitor_people")
public class ExhibitorPerson extends Auditable {

    @ManyToOne
    @JoinColumn(name = "submission_id", nullable = false)
    private ExhibitorSubmission submission;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 150)
    private String designation;

    @Column(nullable = false)
    private boolean printed = false;

    @Column(name = "printed_at")
    private Instant printedAt;

    @ManyToOne
    @JoinColumn(name = "printed_by")
    private User printedBy;

    @Column(nullable = false)
    private boolean issued = false;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @ManyToOne
    @JoinColumn(name = "issued_by")
    private User issuedBy;

    @Column(name = "issued_phone_number", length = 15)
    private String issuedPhoneNumber;
}
