package com.videogen.backend.transcript;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "videogen.transcript")
public record TranscriptProperties(
        String ytDlpPath,
        String whisperCommand,
        Path workDir,
        Duration processTimeout
) {
}
