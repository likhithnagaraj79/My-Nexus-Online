package com.exhibitorreg.crew.badgetemplate.dto;

import com.exhibitorreg.crew.badgetemplate.BadgeTemplate;

public record BadgeTemplateResponse(ElementStyle name, ElementStyle designation, ElementStyle company) {

    public static BadgeTemplateResponse from(BadgeTemplate template) {
        return new BadgeTemplateResponse(
                new ElementStyle(
                        template.getNameXPercent(),
                        template.getNameYPercent(),
                        template.getNameFontSizePt(),
                        template.isNameBold()),
                new ElementStyle(
                        template.getDesignationXPercent(),
                        template.getDesignationYPercent(),
                        template.getDesignationFontSizePt(),
                        template.isDesignationBold()),
                new ElementStyle(
                        template.getCompanyXPercent(),
                        template.getCompanyYPercent(),
                        template.getCompanyFontSizePt(),
                        template.isCompanyBold()));
    }

    /** Sensible starting layout shown before Crew ever saves a template: company near the top,
     * name centered in the middle, designation just below it. */
    public static BadgeTemplateResponse defaultTemplate() {
        return new BadgeTemplateResponse(
                new ElementStyle(50, 45, 20, true),
                new ElementStyle(50, 58, 14, false),
                new ElementStyle(50, 15, 24, true));
    }
}
