package com.axiom.antivpn.common.policy;

import com.axiom.antivpn.api.model.DetectionType;
import com.axiom.antivpn.api.model.VpnResponse;
import com.axiom.antivpn.common.config.Settings;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public final class DetectionPolicy {
    private final @NotNull Settings settings;

    public DetectionPolicy(@NotNull Settings settings) {
        this.settings = settings;
    }

    public @NotNull PolicyDecision evaluate(@NotNull VpnResponse response) {
        if (response.countryCode() != null && settings.getWhitelistedCountries().stream()
                .anyMatch(country -> country.equalsIgnoreCase(response.countryCode()))) {
            return PolicyDecision.allow("country-allowlist");
        }
        DetectionType type = detectionType(response);
        Map.Entry<Integer, EnforcementAction> risk = settings.getRiskActions().floorEntry(response.riskScore());
        boolean thresholdMatched = response.riskScore() >= settings.getRiskScoreThreshold() || risk != null;
        if (type == null && !thresholdMatched) return PolicyDecision.allow("safe");

        EnforcementAction configured = risk != null ? risk.getValue()
                : settings.getTypeActions().getOrDefault(type, settings.getDefaultAction());
        EnforcementAction action = settings.isMonitorMode() ? EnforcementAction.ALERT : configured;
        List<String> commands = action == EnforcementAction.COMMAND ? settings.getActionCommands() : List.of();
        if (action == EnforcementAction.COMMAND && commands.isEmpty()) action = EnforcementAction.ALERT;
        String reason = type == null ? "RISK_SCORE" : type.name();
        return new PolicyDecision(action, true, reason, commands);
    }

    private DetectionType detectionType(VpnResponse response) {
        for (DetectionType type : settings.getBlockedTypes()) {
            if (type.isBlocked(response)) return type;
        }
        return null;
    }
}
