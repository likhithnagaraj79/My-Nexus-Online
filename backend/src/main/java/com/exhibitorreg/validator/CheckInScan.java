package com.exhibitorreg.validator;

import com.exhibitorreg.admin.EventDay;
import com.exhibitorreg.auth.User;
import com.exhibitorreg.common.Auditable;
import com.exhibitorreg.publicregistration.ExhibitorPerson;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "check_in_scans")
public class CheckInScan extends Auditable {

    @ManyToOne
    @JoinColumn(name = "exhibitor_person_id", nullable = false)
    private ExhibitorPerson exhibitorPerson;

    @ManyToOne
    @JoinColumn(name = "event_day_id", nullable = false)
    private EventDay eventDay;

    @ManyToOne
    @JoinColumn(name = "validator_id", nullable = false)
    private User validator;

    @Column(name = "qr_payload", nullable = false, length = 500)
    private String qrPayload;
}
