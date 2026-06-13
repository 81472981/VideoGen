package com.videogen.backend.transcript;

import java.time.Duration;

record TimedCaption(Duration start, Duration end, String text) {
}
