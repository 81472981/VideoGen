# VideoGen

探店视频生成网站的初始模块：输入视频链接，输出按原视频段落组织的文本。

## 结构

- `frontend/`：Vue 3 + Vite 前端
- `backend/`：Java Spring Boot 后端

## 后端依赖

后端通过外部命令处理视频内容：

1. `yt-dlp`：读取视频字幕、下载音频
2. `ffmpeg`：由 `yt-dlp` 抽取音频时使用
3. Whisper 命令：当视频没有字幕时做语音转文本

macOS 可参考：

```bash
brew install openjdk@17 maven yt-dlp ffmpeg
pipx install openai-whisper
```

默认 Whisper 命令为：

```bash
whisper {audio} --language Chinese --output_format vtt --output_dir {outputDir}
```

可通过环境变量覆盖：

```bash
export VIDEOGEN_YTDLP_PATH=yt-dlp
export VIDEOGEN_WHISPER_COMMAND='whisper {audio} --language Chinese --output_format vtt --output_dir {outputDir}'
```

## 启动

前端：

```bash
cd frontend
npm install
npm run dev
```

后端：

```bash
cd backend
mvn spring-boot:run
```

前端开发服务器默认代理 `/api` 到 `http://localhost:8080`。
