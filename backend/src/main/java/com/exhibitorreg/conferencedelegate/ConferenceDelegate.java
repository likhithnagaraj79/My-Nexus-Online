package com.exhibitorreg.conferencedelegate;

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

/** One flat record per registered delegate — unlike exhibitors, there's no company dedup or
 * multi-person-per-submission indirection: each public submission creates exactly one row.
 * {@code link} is null for delegates bulk-imported via CSV, which don't come through any
 * registration link. {@code name} is nullable for the same reason — CSV import allows a blank
 * name through rather than rejecting the row; Crew fills it in later via the edit action. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "conference_delegates")
public class ConferenceDelegate extends Auditable {

    @ManyToOne
    @JoinColumn(name = "link_id")
    private ConferenceDelegateRegistrationLink link;

    @Column(length = 150)
    private String name;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, length = 150)
    private String designation;

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false)
    private boolean printed = false;

    @Column(name = "printed_at")
    private Instant printedAt;

    @ManyToOne
    @JoinColumn(name = "printed_by")
    private User printedBy;
}
