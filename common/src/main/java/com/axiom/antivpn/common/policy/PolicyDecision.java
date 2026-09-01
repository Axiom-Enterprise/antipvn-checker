package com.axiom.antivpn.common.policy;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record PolicyDecision(@NotNull EnforcementAction action, boolean matched, @NotNull String reason, @NotNull List<String> commands) {
    public PolicyDecision {
        commands = List.copyOf(commands);
        if (action == EnforcementAction.COMMAND && commands.isEmpty()) {
            throw new IllegalArgumentException("COMMAND action requires at least one command");
        }
    }

    public static @NotNull PolicyDecision allow(@NotNull String reason) {
        return new PolicyDecision(EnforcementAction.ALLOW, false, reason, List.of());
    }
}
