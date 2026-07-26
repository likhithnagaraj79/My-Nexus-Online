package com.exhibitorreg.common.web;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes a {@link ProblemDetail} JSON body directly to the response. Used by security
 * filters/handlers that run before the DispatcherServlet, where {@code @RestControllerAdvice}
 * cannot intercept the response — keeps the error shape consistent with GlobalExceptionHandler.
 */
public final class ProblemDetailResponseWriter {

    private ProblemDetailResponseWriter() {
    }

    public static void write(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            HttpStatus status,
            String errorCode,
            String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("errorCode", errorCode);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}
