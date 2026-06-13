package com.videogen.backend.config;

import com.videogen.backend.transcript.TranscriptProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TranscriptProperties.class)
public class TranscriptConfig {
}
