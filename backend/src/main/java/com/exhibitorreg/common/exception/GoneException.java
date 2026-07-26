package com.exhibitorreg.common.exception;

/** The resource existed but is no longer available, e.g. an expired public registration link. */
public class GoneException extends RuntimeException {

    public GoneException(String message) {
        super(message);
    }
}
