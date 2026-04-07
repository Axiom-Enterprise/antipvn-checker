package com.axiom.antivpn.api.model;

import org.jetbrains.annotations.NotNull;
import java.util.Map;

public record ApiStatus(
        @NotNull String overallStatus,
        @NotNull Map<String, ServiceStatus> services
) {

    public boolean isOperational() {
        return "operational".equalsIgnoreCase(overallStatus);
    }

    public record ServiceStatus(
            @NotNull String status,
            double responseTimeMs,
            @NotNull String message
    ) {
        public boolean isHealthy() {
            return "operational".equalsIgnoreCase(status);
        }
    }
}
