package com.axiom.antivpn.common.network;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NetworkDecisionRegistry {
    private final ConcurrentHashMap<UUID, NetworkDecision> decisions = new ConcurrentHashMap<>();

    public void accept(@NotNull NetworkDecision decision) {
        decisions.put(decision.playerUuid(), decision);
    }

    public @NotNull Optional<NetworkDecision> consume(@NotNull UUID uuid, @NotNull String ip) {
        NetworkDecision decision = decisions.remove(uuid);
        if (decision == null || !decision.ip().equals(ip)) return Optional.empty();
        long age = Instant.now().getEpochSecond() - decision.issuedAtEpochSecond();
        return age >= -5 && age <= 30 ? Optional.of(decision) : Optional.empty();
    }
}
