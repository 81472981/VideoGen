package com.videogen.backend.transcript;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParagraphAssemblerTest {
    private final ParagraphAssembler assembler = new ParagraphAssembler();

    @Test
    void keepsNearbyCaptionsTogetherAndSplitsOnLongPause() {
        List<TranscriptParagraph> paragraphs = assembler.assemble(List.of(
                new TimedCaption(Duration.ofSeconds(1), Duration.ofSeconds(2), "先看门头"),
                new TimedCaption(Duration.ofSeconds(3), Duration.ofSeconds(4), "很适合拍照"),
                new TimedCaption(Duration.ofSeconds(10), Duration.ofSeconds(12), "再看招牌菜")
        ));

        assertThat(paragraphs).hasSize(2);
        assertThat(paragraphs.get(0).text()).isEqualTo("先看门头很适合拍照");
        assertThat(paragraphs.get(0).startTime()).isEqualTo("00:01");
        assertThat(paragraphs.get(1).text()).isEqualTo("再看招牌菜");
    }
}
