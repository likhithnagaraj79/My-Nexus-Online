package com.exhibitorreg.auth;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import java.util.Base64;
import org.springframework.stereotype.Service;

/** Wraps dev.samstevens.totp for Crew/Validator TOTP secret generation, QR enrollment, and verification. */
@Service
public class TotpService {

    private static final String ISSUER = "Exhibitor Registration";

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();
    private final CodeVerifier codeVerifier =
            new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    public String generateSecret() {
        return secretGenerator.generate();
    }

    /** @return base64-encoded PNG suitable for embedding as a data: URI on the frontend */
    public String buildQrPngBase64(String username, String secret) {
        QrData data = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            return Base64.getEncoder().encodeToString(qrGenerator.generate(data));
        } catch (QrGenerationException e) {
            throw new IllegalStateException("Failed to generate TOTP enrollment QR code", e);
        }
    }

    public boolean verify(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }
}
