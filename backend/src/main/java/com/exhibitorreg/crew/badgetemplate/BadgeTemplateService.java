package com.exhibitorreg.crew.badgetemplate;

import com.exhibitorreg.crew.badgetemplate.dto.BadgeTemplateResponse;
import com.exhibitorreg.crew.badgetemplate.dto.ElementStyle;
import com.exhibitorreg.crew.badgetemplate.dto.SaveBadgeTemplateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Exactly one shared template row — Crew designs one badge layout together, not per-person. */
@Service
public class BadgeTemplateService {

    private final BadgeTemplateRepository badgeTemplateRepository;

    public BadgeTemplateService(BadgeTemplateRepository badgeTemplateRepository) {
        this.badgeTemplateRepository = badgeTemplateRepository;
    }

    @Transactional(readOnly = true)
    public BadgeTemplateResponse getTemplate() {
        return badgeTemplateRepository
                .findFirstByOrderByCreatedAtAsc()
                .map(BadgeTemplateResponse::from)
                .orElseGet(BadgeTemplateResponse::defaultTemplate);
    }

    @Transactional
    public BadgeTemplateResponse saveTemplate(SaveBadgeTemplateRequest request) {
        BadgeTemplate template = badgeTemplateRepository.findFirstByOrderByCreatedAtAsc().orElseGet(BadgeTemplate::new);

        setElement(
                template::setNameXPercent,
                template::setNameYPercent,
                template::setNameFontSizePt,
                template::setNameBold,
                request.name());
        setElement(
                template::setDesignationXPercent,
                template::setDesignationYPercent,
                template::setDesignationFontSizePt,
                template::setDesignationBold,
                request.designation());
        setElement(
                template::setCompanyXPercent,
                template::setCompanyYPercent,
                template::setCompanyFontSizePt,
                template::setCompanyBold,
                request.company());

        badgeTemplateRepository.save(template);
        return BadgeTemplateResponse.from(template);
    }

    private interface DoubleSetter {
        void set(double value);
    }

    private interface BooleanSetter {
        void set(boolean value);
    }

    private void setElement(
            DoubleSetter xSetter, DoubleSetter ySetter, DoubleSetter fontSizeSetter, BooleanSetter boldSetter, ElementStyle style) {
        xSetter.set(style.xPercent());
        ySetter.set(style.yPercent());
        fontSizeSetter.set(style.fontSizePt());
        boldSetter.set(style.bold());
    }
}
