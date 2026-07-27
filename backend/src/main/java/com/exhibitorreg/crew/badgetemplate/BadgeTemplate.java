package com.exhibitorreg.crew.badgetemplate;

import com.exhibitorreg.common.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Exactly one row ever exists — a single shared badge print layout (see V11 migration). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "badge_templates")
public class BadgeTemplate extends Auditable {

    @Column(name = "name_x_percent", nullable = false)
    private double nameXPercent;

    @Column(name = "name_y_percent", nullable = false)
    private double nameYPercent;

    @Column(name = "name_font_size_pt", nullable = false)
    private double nameFontSizePt;

    @Column(name = "name_bold", nullable = false)
    private boolean nameBold;

    @Column(name = "designation_x_percent", nullable = false)
    private double designationXPercent;

    @Column(name = "designation_y_percent", nullable = false)
    private double designationYPercent;

    @Column(name = "designation_font_size_pt", nullable = false)
    private double designationFontSizePt;

    @Column(name = "designation_bold", nullable = false)
    private boolean designationBold;

    @Column(name = "company_x_percent", nullable = false)
    private double companyXPercent;

    @Column(name = "company_y_percent", nullable = false)
    private double companyYPercent;

    @Column(name = "company_font_size_pt", nullable = false)
    private double companyFontSizePt;

    @Column(name = "company_bold", nullable = false)
    private boolean companyBold;
}
