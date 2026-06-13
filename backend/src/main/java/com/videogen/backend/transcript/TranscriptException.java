package com.videogen.backend.transcript;

public class TranscriptException extends RuntimeException {
    public TranscriptException(String message) {
        super(message);
    }

    public TranscriptException(String message, Throwable cause) {
        super(message, cause);
    }
}
