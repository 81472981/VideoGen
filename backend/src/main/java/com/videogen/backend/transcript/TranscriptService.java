package com.videogen.backend.transcript;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class TranscriptService {
    private final TranscriptProperties properties;
    private final ProcessRunner processRunner;
    private final CaptionParser captionParser;
    private final ParagraphAssembler paragraphAssembler;

    public TranscriptService(
            TranscriptProperties properties,
            ProcessRunner processRunner,
            CaptionParser captionParser,
            ParagraphAssembler paragraphAssembler
    ) {
        this.properties = properties;
        this.processRunner = processRunner;
        this.captionParser = captionParser;
        this.paragraphAssembler = paragraphAssembler;
    }

    public TranscriptResponse transcribe(String videoUrl) {
        Path jobDir = createJobDir();

        try {
            Optional<Path> subtitleFile = downloadSubtitle(videoUrl, jobDir);
            if (subtitleFile.isPresent()) {
                return buildResponse(TranscriptSourceType.SUBTITLE, videoUrl, subtitleFile.get());
            }

            Path audioFile = downloadAudio(videoUrl, jobDir);
            Path speechCaptionFile = transcribeAudio(audioFile, jobDir);
            return buildResponse(TranscriptSourceType.SPEECH, videoUrl, speechCaptionFile);
        } finally {
            deleteQuietly(jobDir);
        }
    }

    private TranscriptResponse buildResponse(TranscriptSourceType sourceType, String videoUrl, Path captionFile) {
        List<TimedCaption> captions = captionParser.parse(captionFile);
        List<TranscriptParagraph> paragraphs = paragraphAssembler.assemble(captions);

        if (paragraphs.isEmpty()) {
            throw new TranscriptException("没有提取到可用文本。");
        }

        return new TranscriptResponse(sourceType, videoUrl, paragraphs);
    }

    private Optional<Path> downloadSubtitle(String videoUrl, Path jobDir) {
        List<String> command = List.of(
                properties.ytDlpPath(),
                "--skip-download",
                "--write-subs",
                "--write-auto-subs",
                "--sub-langs", "zh-Hans,zh-Hant,zh,en",
                "--sub-format", "vtt/srt/best",
                "-o", "subtitle.%(ext)s",
                videoUrl
        );

        ProcessResult result = processRunner.run(command, jobDir, properties.processTimeout());
        return findCaptionFile(jobDir)
                .or(() -> {
                    if (result.exitCode() != 0) {
                        return Optional.empty();
                    }
                    return Optional.empty();
                });
    }

    private Path downloadAudio(String videoUrl, Path jobDir) {
        List<String> command = List.of(
                properties.ytDlpPath(),
                "-x",
                "--audio-format", "mp3",
                "--audio-quality", "0",
                "-o", "audio.%(ext)s",
                videoUrl
        );

        ProcessResult result = processRunner.run(command, jobDir, properties.processTimeout());
        if (result.exitCode() != 0) {
            throw new TranscriptException("视频音频下载失败：" + conciseOutput(result.output()));
        }

        return findFileByPrefix(jobDir, "audio.")
                .orElseThrow(() -> new TranscriptException("音频文件生成失败。"));
    }

    private Path transcribeAudio(Path audioFile, Path jobDir) {
        List<String> command = renderWhisperCommand(audioFile, jobDir);
        ProcessResult result = processRunner.run(command, jobDir, properties.processTimeout());
        if (result.exitCode() != 0) {
            throw new TranscriptException("语音识别失败：" + conciseOutput(result.output()));
        }

        return findCaptionFile(jobDir)
                .orElseThrow(() -> new TranscriptException("语音识别没有生成字幕文件。"));
    }

    private List<String> renderWhisperCommand(Path audioFile, Path outputDir) {
        String rendered = properties.whisperCommand()
                .replace("{audio}", audioFile.toAbsolutePath().toString())
                .replace("{outputDir}", outputDir.toAbsolutePath().toString());
        return splitCommand(rendered);
    }

    private List<String> splitCommand(String command) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int index = 0; index < command.length(); index++) {
            char value = command.charAt(index);
            if (value == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (value == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (Character.isWhitespace(value) && !inSingleQuote && !inDoubleQuote) {
                if (!current.isEmpty()) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(value);
            }
        }

        if (!current.isEmpty()) {
            parts.add(current.toString());
        }

        return parts;
    }

    private Optional<Path> findCaptionFile(Path jobDir) {
        try (Stream<Path> paths = Files.list(jobDir)) {
            return paths
                    .filter(path -> {
                        String filename = path.getFileName().toString().toLowerCase();
                        return filename.endsWith(".vtt") || filename.endsWith(".srt");
                    })
                    .max(Comparator.comparingLong(this::fileSize));
        } catch (IOException exception) {
            throw new TranscriptException("查找字幕文件失败。", exception);
        }
    }

    private Optional<Path> findFileByPrefix(Path jobDir, String prefix) {
        try (Stream<Path> paths = Files.list(jobDir)) {
            return paths
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .findFirst();
        } catch (IOException exception) {
            throw new TranscriptException("查找音频文件失败。", exception);
        }
    }

    private long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            return 0L;
        }
    }

    private Path createJobDir() {
        try {
            Path workDir = properties.workDir();
            Files.createDirectories(workDir);
            return Files.createDirectories(workDir.resolve(UUID.randomUUID().toString()));
        } catch (IOException exception) {
            throw new TranscriptException("临时工作目录创建失败。", exception);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            if (!Files.exists(path)) {
                return;
            }
            try (var paths = Files.walk(path)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(item -> {
                            try {
                                Files.deleteIfExists(item);
                            } catch (IOException ignored) {
                            }
                        });
            }
        } catch (IOException ignored) {
        }
    }

    private String conciseOutput(String output) {
        String normalized = output == null ? "" : output.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 220) {
            return normalized;
        }
        return normalized.substring(0, 220) + "...";
    }
}
