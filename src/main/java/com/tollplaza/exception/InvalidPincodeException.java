package com.tollplaza.exception;

/** Pincode is malformed or cannot be resolved to a location in India. */
public class InvalidPincodeException extends RuntimeException {
    public InvalidPincodeException(String message) {
        super(message);
    }
}
