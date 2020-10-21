package com.iri.backend.tools.exception;

public class MongolBaseException extends Exception {
  public MongolBaseException(String message) {
    super(message);
  }

  public MongolBaseException(String message, Throwable cause) {
    super(message, cause);
  }
}
