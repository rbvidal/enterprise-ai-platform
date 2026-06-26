package com.cognitera.platform.api.web;

import com.cognitera.platform.api.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

/**
 * Global exception handler that maps common exceptions to structured error responses.
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    /**
     * Handles authentication failures, returning a 401 response.
     */
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentials(BadCredentialsException ex) {
        return response(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    /**
     * Handles invalid arguments and validation failures, returning a 400 response.
     */
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(Exception ex) {
        return response(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    /**
     * Handles file uploads that exceed the maximum allowed size, returning a 413 response.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ErrorResponse handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "Payload Too Large",
                "File size exceeds the maximum allowed upload size. Max file size: "
                        + ex.getMaxUploadSize() + " bytes.");
    }

    /**
     * Handles multipart file upload failures, returning a 400 response.
     */
    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMultipart(MultipartException ex) {
        return response(HttpStatus.BAD_REQUEST, "Bad Request",
                "File upload failed: " + ex.getMessage());
    }

    /**
     * Catches all unhandled exceptions and returns a 500 response with stacktrace details.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAll(Exception ex) {
        log.error("Unhandled exception", ex);
        return responseWithStacktrace(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error", ex);
    }

    private ErrorResponse response(HttpStatus status, String error, String message) {
        return new ErrorResponse(Instant.now(), status.value(), error, message, null);
    }

    private ErrorResponse responseWithStacktrace(HttpStatus status, String error, Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return new ErrorResponse(Instant.now(), status.value(), error, ex.getMessage(), sw.toString());
    }
}
