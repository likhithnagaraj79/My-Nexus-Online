package com.exhibitorreg.common.exception;

public class CaptchaVerificationException extends RuntimeException {

    public CaptchaVerificationException(String message) {
        super(message);
    }
}
