package com.axiom.antivpn.common.telemetry;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public final class TelemetryFormatter {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public @NotNull String formatStats(@NotNull StatsSnapshot stats) {
        return "{prefix}&7Statistics\n&8 • &7Checks: &f" + stats.checks()
                + "\n&8 • &7Allowed: &a" + stats.allowed()
                + "\n&8 • &7Detected: &c" + stats.blocked()
                + "\n&8 • &7Cache hits: &e" + stats.cacheHits()
                + "\n&8 • &7API failures: &c" + stats.failures()
                + "\n&8 • &7Detections: &f" + compact(stats.detections())
                + "\n&8 • &7Countries: &f" + compact(stats.countries());
    }

    public @NotNull String formatHistory(@NotNull List<CheckRecord> history) {
        if (history.isEmpty()) return "{prefix}&7No history found.";
        StringBuilder out = new StringBuilder("{prefix}&7Recent checks:");
        for (CheckRecord record : history) {
            out.append("\n&8 • &f").append(TIME.format(Instant.ofEpochMilli(record.checkedAt())))
                    .append(" &7").append(record.detection()).append(" &8[&e")
                    .append(record.riskScore()).append("&8] &f").append(record.action());
        }
        return out.toString();
    }

    private String compact(Map<String, Long> values) {
        if (values.isEmpty()) return "none";
        return values.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).limit(5)
                .reduce((a, b) -> a + ", " + b).orElse("none");
    }
}
