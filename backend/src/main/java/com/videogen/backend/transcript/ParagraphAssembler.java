package com.videogen.backend.transcript;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class ParagraphAssembler {
    private static final Duration MAX_GAP_IN_PARAGRAPH = Duration.ofSeconds(3);
    private static final int MAX_CHARS_IN_PARAGRAPH = 180;

    List<TranscriptParagraph> assemble(List<TimedCaption> captions) {
        List<TranscriptParagraph> paragraphs = new ArrayList<>();
        if (captions.isEmpty()) {
            return paragraphs;
        }

        TimedCaption firstCaption = captions.get(0);
        Duration paragraphStart = firstCaption.start();
        Duration paragraphEnd = firstCaption.end();
        StringBuilder text = new StringBuilder(firstCaption.text());

        for (int index = 1; index < captions.size(); index++) {
            TimedCaption caption = captions.get(index);
            Duration gap = caption.start().minus(paragraphEnd);
            boolean shouldSplit = gap.compareTo(MAX_GAP_IN_PARAGRAPH) > 0
                    || text.length() + caption.text().length() > MAX_CHARS_IN_PARAGRAPH
                    || endsParagraph(text);

            if (shouldSplit) {
                paragraphs.add(toParagraph(paragraphs.size() + 1, paragraphStart, paragraphEnd, text.toString()));
                paragraphStart = caption.start();
                text = new StringBuilder(caption.text());
            } else {
                text.append(needsSpace(text, caption.text()) ? " " : "").append(caption.text());
            }

            paragraphEnd = caption.end();
        }

        paragraphs.add(toParagraph(paragraphs.size() + 1, paragraphStart, paragraphEnd, text.toString()));
        return paragraphs;
    }

    private boolean endsParagraph(StringBuilder text) {
        if (text.isEmpty()) {
            return false;
        }

        char last = text.charAt(text.length() - 1);
        return last == '。' || last == '！' || last == '？' || last == '.' || last == '!' || last == '?';
    }

    private boolean needsSpace(StringBuilder current, String nextText) {
        if (current.isEmpty() || nextText.isBlank()) {
            return false;
        }

        char last = current.charAt(current.length() - 1);
        char next = nextText.charAt(0);
        return isAsciiLetterOrDigit(last) && isAsciiLetterOrDigit(next);
    }

    private boolean isAsciiLetterOrDigit(char value) {
        return (value >= 'a' && value <= 'z')
                || (value >= 'A' && value <= 'Z')
                || (value >= '0' && value <= '9');
    }

    private TranscriptParagraph toParagraph(int index, Duration start, Duration end, String text) {
        return new TranscriptParagraph(index, formatTime(start), formatTime(end), text.trim());
    }

    private String formatTime(Duration duration) {
        long totalSeconds = duration.toSeconds();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }
}
