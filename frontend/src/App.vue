<script setup>
import { computed, ref } from 'vue'

const videoUrl = ref('')
const isLoading = ref(false)
const result = ref(null)
const errorMessage = ref('')
const progress = ref(0)
const progressStage = ref('准备整理')
let progressTimer = null

const canSubmit = computed(() => videoUrl.value.trim().length > 0 && !isLoading.value)

async function submitTranscription() {
  if (!canSubmit.value) return

  isLoading.value = true
  result.value = null
  errorMessage.value = ''
  startProgress()

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
    completeProgress()
  } catch (error) {
    errorMessage.value = error.message
    stopProgress()
  } finally {
    isLoading.value = false
  }
}

function startProgress() {
  clearProgressTimer()
  progress.value = 8
  progressStage.value = '正在读取视频'

  progressTimer = window.setInterval(() => {
    if (progress.value < 38) {
      progress.value += 4
      progressStage.value = '正在整理内容'
      return
    }

    if (progress.value < 72) {
      progress.value += 2
      progressStage.value = '正在生成文本'
      return
    }

    if (progress.value < 92) {
      progress.value += 1
      progressStage.value = '正在优化段落'
    }
  }, 1200)
}

function completeProgress() {
  clearProgressTimer()
  progress.value = 100
  progressStage.value = '整理完成'
}

function stopProgress() {
  clearProgressTimer()
  progress.value = 0
  progressStage.value = '准备整理'
}

function clearProgressTimer() {
  if (progressTimer) {
    window.clearInterval(progressTimer)
    progressTimer = null
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
      <header class="topbar">
        <div class="brand">
          <span class="brand-mark">V</span>
          <span>VideoGen</span>
        </div>
        <span class="status-pill">探店内容创作助手</span>
      </header>

      <section class="hero">
        <div class="intro">
          <p class="eyebrow">为探店创作者提效</p>
          <h1>把视频链接快速整理成可编辑文案</h1>
          <p class="summary">
            粘贴一条视频链接，获得层次清晰、方便二次创作的文本内容，让脚本整理和文案改写更顺手。
          </p>

          <div class="metric-row" aria-label="内容优势">
            <span>一键整理</span>
            <span>段落清晰</span>
            <span>方便改写</span>
          </div>
        </div>

      </section>

      <form class="input-panel" @submit.prevent="submitTranscription">
        <div class="form-heading">
          <div>
            <label for="videoUrl">视频链接</label>
            <p>粘贴你想整理的公开视频链接。</p>
          </div>
        </div>
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
        <p class="hint">视频越长，整理时间越久。处理完成后会按段落展示结果。</p>
      </form>

      <section v-if="errorMessage" class="notice error" role="alert">
        {{ errorMessage }}
      </section>

      <section v-if="isLoading" class="notice loading">
        <div class="progress-header">
          <span>{{ progressStage }}</span>
          <strong>{{ progress }}%</strong>
        </div>
        <div class="progress-track" role="progressbar" :aria-valuenow="progress" aria-valuemin="0" aria-valuemax="100">
          <div class="progress-fill" :style="{ width: `${progress}%` }"></div>
        </div>
        <p>视频越长，等待时间越久。页面保持打开即可。</p>
      </section>

      <section v-if="result" class="result-panel">
        <div class="result-header">
          <div>
            <p class="eyebrow">整理完成</p>
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
