package com.videogen.backend.transcript;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record TranscriptRequest(
        @NotBlank(message = "视频链接不能为空")
        @URL(message = "请输入合法的视频链接")
        String videoUrl
) {
}
