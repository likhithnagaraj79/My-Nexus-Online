package com.exhibitorreg.common;

import com.exhibitorreg.common.exception.AccountLockedException;
import com.exhibitorreg.common.exception.BusinessRuleViolationException;
import com.exhibitorreg.common.exception.CaptchaVerificationException;
import com.exhibitorreg.common.exception.ConflictException;
import com.exhibitorreg.common.exception.GoneException;
import com.exhibitorreg.common.exception.InvalidCredentialsException;
import com.exhibitorreg.common.exception.NotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(GoneException.class)
    public ProblemDetail handleGone(GoneException ex) {
        return problem(HttpStatus.GONE, "GONE", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return problem(HttpStatus.CONFLICT, "CONFLICT", "The request conflicts with existing data.");
    }

    @ExceptionHandler(AccountLockedException.class)
    public ProblemDetail handleAccountLocked(AccountLockedException ex) {
        return problem(HttpStatus.LOCKED, "ACCOUNT_LOCKED", ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage());
    }

    @ExceptionHandler(CaptchaVerificationException.class)
    public ProblemDetail handleCaptchaVerification(CaptchaVerificationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "CAPTCHA_FAILED", ex.getMessage());
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "BUSINESS_RULE_VIOLATION", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed.");
        problem.setProperty("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    private static ProblemDetail problem(HttpStatus status, String errorCode, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("errorCode", errorCode);
        return problem;
    }
}
