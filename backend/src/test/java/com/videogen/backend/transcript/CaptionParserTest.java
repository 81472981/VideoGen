package com.videogen.backend.transcript;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CaptionParserTest {
    private final CaptionParser parser = new CaptionParser();

    @Test
    void parsesVttCaptions() throws Exception {
        Path file = Files.createTempFile("captions", ".vtt");
        Files.writeString(file, """
                WEBVTT

                00:00:01.000 --> 00:00:03.000
                今天我们来探一家新开的咖啡店

                00:00:04.000 --> 00:00:06.000
                <c>门头很有设计感</c>
                """);

        List<TimedCaption> captions = parser.parse(file);

        assertThat(captions).hasSize(2);
        assertThat(captions.get(0).text()).isEqualTo("今天我们来探一家新开的咖啡店");
        assertThat(captions.get(1).text()).isEqualTo("门头很有设计感");
    }
}
