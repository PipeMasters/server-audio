package com.pipemasters.serveraudio.exceptions.audio;


public class AudioExtractionException extends RuntimeException {
    public AudioExtractionException(String message) {
        super(message);
    }

    public AudioExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
