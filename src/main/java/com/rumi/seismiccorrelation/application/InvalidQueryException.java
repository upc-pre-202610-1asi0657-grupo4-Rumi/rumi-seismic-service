package com.rumi.seismiccorrelation.application;

/** A query whose parameters are well formed but inconsistent, for example a reversed time range. */
public class InvalidQueryException extends IllegalArgumentException {

    public InvalidQueryException(String message) {
        super(message);
    }
}
