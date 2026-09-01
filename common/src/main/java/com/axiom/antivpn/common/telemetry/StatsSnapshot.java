package com.axiom.antivpn.common.telemetry;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public record StatsSnapshot(long checks, long allowed, long blocked, long cacheHits, long failures, @NotNull Map<String, Long> detections, @NotNull Map<String, Long> countries) {
}
