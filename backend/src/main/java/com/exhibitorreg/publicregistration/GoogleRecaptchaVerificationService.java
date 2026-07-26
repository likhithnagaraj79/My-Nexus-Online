package com.exhibitorreg.publicregistration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** Server-side verification against Google's reCAPTCHA siteverify endpoint. The frontend
 * already committed to react-google-recaptcha (Phase 1), so this is the only consistent
 * captcha approach rather than a home-grown challenge. */
@Service
public class GoogleRecaptchaVerificationService implements RecaptchaVerificationService {

    private static final String VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final RestClient restClient;
    private final String secretKey;
    private final boolean enabled;

    public GoogleRecaptchaVerificationService(
            RestClient.Builder restClientBuilder,
            @Value("${app.recaptcha.secret-key}") String secretKey,
            @Value("${app.recaptcha.enabled}") boolean enabled) {
        this.restClient = restClientBuilder.build();
        this.secretKey = secretKey;
        this.enabled = enabled;
    }

    @Override
    public boolean verify(String token, String remoteIp) {
        if (!enabled) {
            return true;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secretKey);
        form.add("response", token);
        if (remoteIp != null) {
            form.add("remoteip", remoteIp);
        }

        SiteVerifyResponse response = restClient.post()
                .uri(VERIFY_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(SiteVerifyResponse.class);

        return response != null && response.success();
    }

    private record SiteVerifyResponse(boolean success) {
    }
}
