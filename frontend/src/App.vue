<script setup>
import { computed, ref } from 'vue'

const videoUrl = ref('')
const isLoading = ref(false)
const result = ref(null)
const errorMessage = ref('')

const canSubmit = computed(() => videoUrl.value.trim().length > 0 && !isLoading.value)

async function submitTranscription() {
  if (!canSubmit.value) return

  isLoading.value = true
  result.value = null
  errorMessage.value = ''

  try {
    const response = await fetch('/api/transcriptions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ videoUrl: videoUrl.value.trim() })
    })

    const responseText = await response.text()
    const payload = parseJson(responseText)
    if (!response.ok) {
      throw new Error(payload?.message || responseText || `视频文本提取失败，状态码：${response.status}`)
    }

    result.value = payload
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    isLoading.value = false
  }
}

function parseJson(text) {
  if (!text) return null

  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}
</script>

<template>
  <main class="page-shell">
    <section class="workspace">
      <div class="intro">
        <p class="eyebrow">探店视频生成 · 第一个模块</p>
        <h1>把视频链接转成可编辑文案</h1>
        <p class="summary">
          优先提取视频自带字幕；没有字幕时自动走语音识别，并按时间段整理成段落。
        </p>
      </div>

      <form class="input-panel" @submit.prevent="submitTranscription">
        <label for="videoUrl">视频链接</label>
        <div class="input-row">
          <input
            id="videoUrl"
            v-model="videoUrl"
            type="url"
            placeholder="粘贴抖音、小红书、B站或公开视频链接"
            autocomplete="off"
          />
          <button type="submit" :disabled="!canSubmit">
            {{ isLoading ? '提取中...' : '提取文本' }}
          </button>
        </div>
        <p class="hint">后端会先找字幕；找不到字幕时再下载音频进行语音转写。</p>
      </form>

      <section v-if="errorMessage" class="notice error" role="alert">
        {{ errorMessage }}
      </section>

      <section v-if="isLoading" class="notice loading">
        正在解析视频，这一步取决于视频长度和平台响应速度。
      </section>

      <section v-if="result" class="result-panel">
        <div class="result-header">
          <div>
            <p class="eyebrow">来源：{{ result.sourceType === 'SUBTITLE' ? '视频字幕' : '语音识别' }}</p>
            <h2>提取结果</h2>
          </div>
          <span>{{ result.paragraphs.length }} 段</span>
        </div>

        <article v-for="paragraph in result.paragraphs" :key="paragraph.index" class="paragraph">
          <time>{{ paragraph.startTime }} - {{ paragraph.endTime }}</time>
          <p>{{ paragraph.text }}</p>
        </article>
      </section>
    </section>
  </main>
</template>
