package com.videogen.backend.transcript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
class ProcessRunner {
    ProcessResult run(List<String> command, Path workDir, Duration timeout) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workDir.toFile());
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> readOutput(process));
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new TranscriptException("处理超时，请换一个更短的视频或稍后重试。");
            }

            String output = outputFuture.join();
            return new ProcessResult(process.exitValue(), output);
        } catch (IOException exception) {
            throw new TranscriptException("无法启动外部处理命令，请确认 yt-dlp、ffmpeg 和 Whisper 已安装。", exception);
        } catch (CompletionException exception) {
            throw new TranscriptException("读取外部处理命令输出失败。", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TranscriptException("视频处理被中断。", exception);
        }
    }

    private String readOutput(Process process) {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        } catch (IOException exception) {
            throw new CompletionException(exception);
        }
        return output.toString();
    }
}
