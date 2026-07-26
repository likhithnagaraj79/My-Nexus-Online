package com.exhibitorreg.common.exception;

/** Generic catch-all for business-rule violations that don't fit Bean Validation, e.g. the
 * labour-pass stall-number requirement when it needs to fire outside declarative @Valid binding. */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
