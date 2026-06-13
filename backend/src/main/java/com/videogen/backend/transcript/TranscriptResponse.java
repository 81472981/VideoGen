package com.videogen.backend.transcript;

import java.util.List;

public record TranscriptResponse(
        TranscriptSourceType sourceType,
        String videoUrl,
        List<TranscriptParagraph> paragraphs
) {
}
