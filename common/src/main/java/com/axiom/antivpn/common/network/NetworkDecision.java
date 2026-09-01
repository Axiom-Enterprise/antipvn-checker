package com.axiom.antivpn.common.network;

import com.axiom.antivpn.common.policy.EnforcementAction;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record NetworkDecision(@NotNull UUID playerUuid, @NotNull String ip, @NotNull EnforcementAction action, @NotNull String reason, int riskScore, long issuedAtEpochSecond) {
}
