package com.videogen.backend.transcript;

public record TranscriptParagraph(
        int index,
        String startTime,
        String endTime,
        String text
) {
}
