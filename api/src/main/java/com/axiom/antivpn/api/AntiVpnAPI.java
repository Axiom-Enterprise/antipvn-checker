package com.axiom.antivpn.api;

import com.axiom.antivpn.api.model.ApiStatus;
import com.axiom.antivpn.api.model.VpnResponse;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AntiVpnAPI {

    @NotNull CompletableFuture<VpnResponse> checkIp(@NotNull String ip);

    @NotNull CompletableFuture<List<VpnResponse>> checkBatch(@NotNull List<String> ips);

    @NotNull CompletableFuture<ApiStatus> getStatus();

    boolean isWhitelisted(@NotNull String ip);

    boolean isWhitelisted(@NotNull UUID uuid);

    void whitelistIp(@NotNull String ip);

    void whitelistPlayer(@NotNull UUID uuid);

    void removeWhitelistIp(@NotNull String ip);

    void removeWhitelistPlayer(@NotNull UUID uuid);

    void invalidateCache(@NotNull String ip);

    void clearCache();
}
