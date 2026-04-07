package com.axiom.antivpn.api.event;

import com.axiom.antivpn.api.model.VpnResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class IpCheckEvent {

    private final @NotNull String ip;
    private final @Nullable UUID playerUuid;
    private final @Nullable String playerName;
    private final @NotNull VpnResponse response;
    private boolean cancelled;

    public IpCheckEvent(@NotNull String ip,
                        @Nullable UUID playerUuid,
                        @Nullable String playerName,
                        @NotNull VpnResponse response) {
        this.ip = ip;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.response = response;
    }

    public @NotNull String getIp() {
        return ip;
    }

    public @Nullable UUID getPlayerUuid() {
        return playerUuid;
    }

    public @Nullable String getPlayerName() {
        return playerName;
    }

    public @NotNull VpnResponse getResponse() {
        return response;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
