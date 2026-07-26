package com.exhibitorreg.auth;

import static org.assertj.core.api.Assertions.assertThat;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.Test;

class TotpServiceTest {

    private final TotpService totpService = new TotpService();

    @Test
    void generatesASecretOfPlausibleLength() {
        String secret = totpService.generateSecret();

        assertThat(secret).isNotBlank();
        assertThat(secret.length()).isGreaterThanOrEqualTo(16);
    }

    @Test
    void buildsANonEmptyQrPng() {
        String secret = totpService.generateSecret();

        String base64Png = totpService.buildQrPngBase64("crew1", secret);

        assertThat(base64Png).isNotBlank();
        assertThat(java.util.Base64.getDecoder().decode(base64Png)).isNotEmpty();
    }

    @Test
    void verifiesACodeGeneratedForTheSameSecret() throws Exception {
        String secret = totpService.generateSecret();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        String validCode = codeGenerator.generate(secret, System.currentTimeMillis() / 1000L / 30L);

        assertThat(totpService.verify(secret, validCode)).isTrue();
        assertThat(totpService.verify(secret, "000000")).isFalse();
    }
}
