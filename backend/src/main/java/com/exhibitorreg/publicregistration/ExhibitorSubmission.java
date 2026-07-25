package com.exhibitorreg.publicregistration;

import com.exhibitorreg.common.Auditable;
import com.exhibitorreg.organiser.ExhibitorRegistrationLink;
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
@Table(name = "exhibitor_submissions")
public class ExhibitorSubmission extends Auditable {

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne
    @JoinColumn(name = "link_id", nullable = false)
    private ExhibitorRegistrationLink link;

    @Column(name = "declared_count", nullable = false)
    private int declaredCount;
}
