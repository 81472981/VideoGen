package com.videogen.backend.transcript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class CaptionParser {
    private static final Pattern TIMING_LINE = Pattern.compile(
            "^(?<start>\\d{2}:\\d{2}:\\d{2}[,.]\\d{3}|\\d{2}:\\d{2}[,.]\\d{3})\\s+-->\\s+(?<end>\\d{2}:\\d{2}:\\d{2}[,.]\\d{3}|\\d{2}:\\d{2}[,.]\\d{3}).*$");
    private static final Pattern TAGS = Pattern.compile("<[^>]+>");

    List<TimedCaption> parse(Path captionFile) {
        try {
            List<String> lines = Files.readAllLines(captionFile, StandardCharsets.UTF_8);
            List<TimedCaption> captions = new ArrayList<>();

            for (int index = 0; index < lines.size(); index++) {
                Matcher matcher = TIMING_LINE.matcher(lines.get(index).trim());
                if (!matcher.matches()) {
                    continue;
                }

                Duration start = parseTime(matcher.group("start"));
                Duration end = parseTime(matcher.group("end"));
                StringBuilder text = new StringBuilder();

                index++;
                while (index < lines.size() && StringUtils.hasText(lines.get(index))) {
                    String line = cleanText(lines.get(index));
                    if (StringUtils.hasText(line)) {
                        if (!text.isEmpty()) {
                            text.append(' ');
                        }
                        text.append(line);
                    }
                    index++;
                }

                if (!text.isEmpty()) {
                    captions.add(new TimedCaption(start, end, text.toString()));
                }
            }

            return captions;
        } catch (IOException exception) {
            throw new TranscriptException("字幕文件读取失败。", exception);
        }
    }

    private Duration parseTime(String value) {
        String normalized = value.replace(',', '.');
        String[] timeAndMillis = normalized.split("\\.");
        String[] parts = timeAndMillis[0].split(":");
        long millis = timeAndMillis.length > 1 ? Long.parseLong(timeAndMillis[1]) : 0L;

        if (parts.length == 2) {
            return Duration.ofMinutes(Long.parseLong(parts[0]))
                    .plusSeconds(Long.parseLong(parts[1]))
                    .plusMillis(millis);
        }

        return Duration.ofHours(Long.parseLong(parts[0]))
                .plusMinutes(Long.parseLong(parts[1]))
                .plusSeconds(Long.parseLong(parts[2]))
                .plusMillis(millis);
    }

    private String cleanText(String line) {
        return TAGS.matcher(line)
                .replaceAll("")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .trim();
    }
}
