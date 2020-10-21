package com.iri.backend.tools.exception;

public class MongolConnectionException extends MongolBaseException {
    public MongolConnectionException(String message, Exception baseException) {
        super(message, baseException);
    }

    public MongolConnectionException(String message) {
        super(message, null);
    }
}
