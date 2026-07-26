package com.exhibitorreg.publicregistration;

public interface RecaptchaVerificationService {

    boolean verify(String token, String remoteIp);
}
