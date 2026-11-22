package com.tollplaza.exception;

/** An upstream provider (geocoder / routing engine) failed or returned unusable data. */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
