package com.rumi.seismiccorrelation.application;

/** The IGP feed failed (or its circuit is open) and there is no stored event to fall back to. */
public class SeismicFeedUnavailableException extends RuntimeException {

    public SeismicFeedUnavailableException() {
        super("The IGP feed is unavailable and no seismic event is stored yet");
    }
}
