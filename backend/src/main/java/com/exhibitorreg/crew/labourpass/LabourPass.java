package com.exhibitorreg.crew.labourpass;

import com.exhibitorreg.admin.Event;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "labour_passes")
public class LabourPass extends Auditable {

    @Enumerated(EnumType.STRING)
    @Column(name = "pass_type", nullable = false, length = 20)
    private LabourPassType passType;

    @Column(name = "pass_count", nullable = false)
    private int passCount;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    /** Nullable at the DB level; required at the service layer for EXHIBITOR and FABRICATOR_LABOUR. */
    @Column(name = "stall_number", length = 20)
    private String stallNumber;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "issued_by", nullable = false)
    private User issuedBy;
}
