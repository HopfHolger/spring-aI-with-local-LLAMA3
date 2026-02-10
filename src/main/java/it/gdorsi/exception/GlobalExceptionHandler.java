package it.gdorsi.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;

/**
 * Durch das Frontend mit Thymeleaf funktioniert das ProblemDetail nicht mehr.
 */
//@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    //@ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail problemDetailillegalArgumentException(IllegalArgumentException e, WebRequest request) {
        ProblemDetail problemDetail
                = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());

        problemDetail.setInstance(URI.create(((ServletWebRequest) request).getRequest().getRequestURI()));
        return problemDetail;
    }

    //@ExceptionHandler(Exception.class)
    public ProblemDetail problemDetailiException(Exception e, WebRequest request) {
        ProblemDetail problemDetail
                = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problemDetail.setInstance(URI.create(((ServletWebRequest) request).getRequest().getRequestURI()));
        return problemDetail;
    }
}
