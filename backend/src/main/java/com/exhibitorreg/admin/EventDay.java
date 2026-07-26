package com.exhibitorreg.admin;

import com.exhibitorreg.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "event_days")
public class EventDay extends Auditable {

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    /** No longer required — days are auto-created as Day 1/2/3 with their Event, with no
     * calendar date attached. Kept (nullable) rather than dropped so historical rows in an
     * already-deployed database still read back their original date. */
    private LocalDate date;
}
