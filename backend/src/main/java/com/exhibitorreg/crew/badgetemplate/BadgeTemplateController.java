package com.exhibitorreg.crew.badgetemplate;

import com.exhibitorreg.crew.badgetemplate.dto.BadgeTemplateResponse;
import com.exhibitorreg.crew.badgetemplate.dto.SaveBadgeTemplateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crew/badge-template")
public class BadgeTemplateController {

    private final BadgeTemplateService badgeTemplateService;

    public BadgeTemplateController(BadgeTemplateService badgeTemplateService) {
        this.badgeTemplateService = badgeTemplateService;
    }

    @GetMapping
    public BadgeTemplateResponse getTemplate() {
        return badgeTemplateService.getTemplate();
    }

    @PutMapping
    public BadgeTemplateResponse saveTemplate(@Valid @RequestBody SaveBadgeTemplateRequest request) {
        return badgeTemplateService.saveTemplate(request);
    }
}
