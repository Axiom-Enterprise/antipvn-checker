package com.axiom.antivpn.common.telemetry;

import com.axiom.antivpn.common.policy.EnforcementAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record CheckRecord(long checkedAt, @Nullable UUID playerUuid, @NotNull String playerName, @NotNull String ip, @Nullable String countryCode, @NotNull String detection, int riskScore, @NotNull EnforcementAction action, boolean matched, boolean cacheHit, boolean apiFailure) {
}
