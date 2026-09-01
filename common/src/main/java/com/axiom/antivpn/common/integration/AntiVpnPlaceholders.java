package com.axiom.antivpn.common.integration;

import com.axiom.antivpn.api.model.VpnResponse;
import com.axiom.antivpn.common.policy.PolicyDecision;
import org.jetbrains.annotations.NotNull;

public final class AntiVpnPlaceholders {
    public @NotNull String resolve(@NotNull String key, @NotNull VpnResponse response, @NotNull PolicyDecision decision) {
        return switch (key.toLowerCase(java.util.Locale.ROOT)) {
            case "risk_score" -> Integer.toString(response.riskScore());
            case "country" -> value(response.country());
            case "country_code" -> value(response.countryCode());
            case "isp" -> value(response.isp());
            case "detection" -> decision.reason();
            case "action" -> decision.action().name();
            case "cache_hit" -> Boolean.toString(response.cached());
            default -> "";
        };
    }

    private static String value(String value) { return value == null ? "Unknown" : value; }
}
