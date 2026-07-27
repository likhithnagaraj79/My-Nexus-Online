package com.exhibitorreg.crew.badgetemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exhibitorreg.crew.badgetemplate.dto.BadgeTemplateResponse;
import com.exhibitorreg.crew.badgetemplate.dto.ElementStyle;
import com.exhibitorreg.crew.badgetemplate.dto.SaveBadgeTemplateRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BadgeTemplateServiceTest {

    @Mock
    private BadgeTemplateRepository badgeTemplateRepository;

    private BadgeTemplateService service;

    @BeforeEach
    void setUp() {
        service = new BadgeTemplateService(badgeTemplateRepository);
    }

    private static SaveBadgeTemplateRequest sampleRequest() {
        return new SaveBadgeTemplateRequest(
                new ElementStyle(10, 20, 18, true),
                new ElementStyle(30, 40, 12, false),
                new ElementStyle(50, 5, 26, true));
    }

    @Test
    void getTemplateReturnsBuiltInDefaultWhenNoneSaved() {
        when(badgeTemplateRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());

        BadgeTemplateResponse response = service.getTemplate();

        assertThat(response).isEqualTo(BadgeTemplateResponse.defaultTemplate());
    }

    @Test
    void getTemplateReturnsTheSavedRowWhenOneExists() {
        BadgeTemplate saved = new BadgeTemplate();
        saved.setNameXPercent(11);
        saved.setNameYPercent(22);
        saved.setNameFontSizePt(15);
        saved.setNameBold(true);
        saved.setDesignationXPercent(33);
        saved.setDesignationYPercent(44);
        saved.setDesignationFontSizePt(11);
        saved.setDesignationBold(false);
        saved.setCompanyXPercent(55);
        saved.setCompanyYPercent(66);
        saved.setCompanyFontSizePt(20);
        saved.setCompanyBold(true);
        when(badgeTemplateRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(saved));

        BadgeTemplateResponse response = service.getTemplate();

        assertThat(response.name()).isEqualTo(new ElementStyle(11, 22, 15, true));
        assertThat(response.designation()).isEqualTo(new ElementStyle(33, 44, 11, false));
        assertThat(response.company()).isEqualTo(new ElementStyle(55, 66, 20, true));
    }

    @Test
    void savingWhenNoRowExistsCreatesOne() {
        when(badgeTemplateRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());

        BadgeTemplateResponse response = service.saveTemplate(sampleRequest());

        ArgumentCaptor<BadgeTemplate> captor = ArgumentCaptor.forClass(BadgeTemplate.class);
        verify(badgeTemplateRepository).save(captor.capture());
        assertThat(captor.getValue().getNameXPercent()).isEqualTo(10);
        assertThat(response.company().fontSizePt()).isEqualTo(26);
    }

    @Test
    void savingASecondTimeUpdatesTheSameRowRatherThanCreatingAnother() {
        BadgeTemplate existing = new BadgeTemplate();
        when(badgeTemplateRepository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));

        service.saveTemplate(sampleRequest());

        ArgumentCaptor<BadgeTemplate> captor = ArgumentCaptor.forClass(BadgeTemplate.class);
        verify(badgeTemplateRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(existing.getDesignationFontSizePt()).isEqualTo(12);
        assertThat(existing.isCompanyBold()).isTrue();
    }
}
