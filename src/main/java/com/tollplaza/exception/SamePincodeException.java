package com.tollplaza.exception;

public class SamePincodeException extends RuntimeException {
    public SamePincodeException() {
        super("Source and destination pincodes cannot be the same");
    }
}
