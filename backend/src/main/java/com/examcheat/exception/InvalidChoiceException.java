package com.examcheat.exception;

/**
 * OOP concept: USER-DEFINED EXCEPTION.
 * Thrown for an invalid menu choice / answer / input (same class as the console project).
 * The REST layer turns it into HTTP 400.
 */
public class InvalidChoiceException extends RuntimeException {
    public InvalidChoiceException(String message) {
        super(message);
    }
}
