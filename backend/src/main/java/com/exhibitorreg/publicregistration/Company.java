package com.exhibitorreg.publicregistration;

import com.exhibitorreg.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "companies")
public class Company extends Auditable {

    @Column(nullable = false, length = 200)
    private String name;
}
