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

    /** Sensible starting layout shown before Crew ever saves a template: all three elements
     * centered horizontally, and centered as a group within the badge stock's printable band
     * (8.6cm-13.8cm of the 15.3cm height — text above or below that is hidden by the badge
     * holder). */
    public static BadgeTemplateResponse defaultTemplate() {
        return new BadgeTemplateResponse(
                new ElementStyle(50, 73.20, 20, true),
                new ElementStyle(50, 81.70, 14, false),
                new ElementStyle(50, 64.71, 24, true));
    }
}
