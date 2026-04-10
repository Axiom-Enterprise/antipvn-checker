package com.axiom.antivpn.common;

import com.axiom.antivpn.api.AntiVpnAPI;
import com.axiom.antivpn.api.AntiVpnProvider;
import com.axiom.antivpn.api.event.EventBus;
import com.axiom.antivpn.api.event.IpCheckEvent;
import com.axiom.antivpn.api.model.ApiStatus;
import com.axiom.antivpn.api.model.DetectionType;
import com.axiom.antivpn.api.model.VpnResponse;
import com.axiom.antivpn.common.cache.VpnCache;
import com.axiom.antivpn.common.check.WhitelistManager;
import com.axiom.antivpn.common.config.Messages;
import com.axiom.antivpn.common.config.PluginConfig;
import com.axiom.antivpn.common.config.Settings;
import com.axiom.antivpn.common.http.HttpClient;
import com.axiom.antivpn.common.http.ResponseParser;
import com.axiom.antivpn.common.platform.Platform;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class AntiVpnEngine implements AntiVpnAPI {

    private final @NotNull Platform platform;
    private final @NotNull Settings settings;
    private final @NotNull Messages messages;
    private final @NotNull VpnCache cache;
    private final @NotNull WhitelistManager whitelist;
    private final @NotNull HttpClient httpClient;

    public AntiVpnEngine(@NotNull Platform platform, @NotNull PluginConfig mainConfig, @NotNull PluginConfig messagesConfig) {
        this.platform = platform;
        this.settings = new Settings(mainConfig);
        this.messages = new Messages(messagesConfig);
        this.cache = new VpnCache(settings);
        this.whitelist = new WhitelistManager(mainConfig);
        this.httpClient = new HttpClient(settings, platform.getAsyncExecutor(), platform.getPluginLogger());

        AntiVpnProvider.register(this);
    }

    @Override
    public @NotNull CompletableFuture<VpnResponse> checkIp(@NotNull String ip) {
        VpnResponse cached = cache.get(ip);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return httpClient.getAsync("/check/" + ip).thenApply(json -> {
            VpnResponse response = ResponseParser.parseCheckResponse(json);
            cache.put(ip, response);
            return response;
        });
    }

    @Override
    public @NotNull CompletableFuture<List<VpnResponse>> checkBatch(@NotNull List<String> ips) {
        JsonObject body = new JsonObject();
        JsonArray arr = new JsonArray();
        for (String ip : ips) {
            arr.add(ip);
        }
        body.add("ips", arr);
        return httpClient.postAsync("/check/batch", body.toString()).thenApply(json -> {
            List<VpnResponse> responses = ResponseParser.parseBatchResponse(json);
            for (VpnResponse r : responses) {
                cache.put(r.ip(), r);
            }
            return responses;
        });
    }

    @Override
    public @NotNull CompletableFuture<ApiStatus> getStatus() {
        return httpClient.getAsync("/status").thenApply(ResponseParser::parseStatusResponse);
    }

    @Override
    public boolean isWhitelisted(@NotNull String ip) {
        return whitelist.isIpWhitelisted(ip);
    }

    @Override
    public boolean isWhitelisted(@NotNull UUID uuid) {
        return whitelist.isPlayerWhitelisted(uuid);
    }

    @Override
    public void whitelistIp(@NotNull String ip) {
        whitelist.addIp(ip);
    }

    @Override
    public void whitelistPlayer(@NotNull UUID uuid) {
        whitelist.addPlayer(uuid);
    }

    @Override
    public void removeWhitelistIp(@NotNull String ip) {
        whitelist.removeIp(ip);
    }

    @Override
    public void removeWhitelistPlayer(@NotNull UUID uuid) {
        whitelist.removePlayer(uuid);
    }

    @Override
    public void invalidateCache(@NotNull String ip) {
        cache.invalidate(ip);
    }

    @Override
    public void clearCache() {
        cache.clear();
    }

    public @NotNull CompletableFuture<Void> handlePlayerJoin(@NotNull UUID uuid, @NotNull String name, @NotNull String ip) {
        if (settings.getApiKey().isEmpty()) {
            platform.getPluginLogger().warning("API key not configured. Skipping VPN check for " + name);
            return CompletableFuture.completedFuture(null);
        }

        if (whitelist.isIpWhitelisted(ip) || whitelist.isPlayerWhitelisted(uuid)) {
            return CompletableFuture.completedFuture(null);
        }

        if (platform.hasPermission(uuid, "antivpn.bypass")) {
            return CompletableFuture.completedFuture(null);
        }

        return checkIp(ip)
                .thenAccept(response -> handleCheckResult(uuid, name, ip, response))
                .exceptionally(ex -> {
                    platform.getPluginLogger().log(Level.WARNING, "Failed to check IP " + ip + " for " + name + ": " + ex.getMessage());
                    if (settings.isBlockOnApiFailure()) {
                        platform.kickPlayer(uuid, messages.format(messages.getKickMessage()));
                    }
                    return null;
                });
    }

    private void handleCheckResult(@NotNull UUID uuid, @NotNull String name,
                                   @NotNull String ip, @NotNull VpnResponse response) {
        IpCheckEvent event = new IpCheckEvent(ip, uuid, name, response);
        EventBus.get().fire(event);
        if (event.isCancelled()) {
            return;
        }

        if (!shouldBlock(response)) {
            return;
        }

        String kickMsg = messages.format(messages.getKickMessage(), response);
        platform.kickPlayer(uuid, kickMsg);

        if (settings.isAlertsEnabled()) {
            String alertMsg = messages.format(
                    messages.getAlertMessage(), response)
                    .replace("{player}", name);
            platform.broadcastPermission(settings.getAlertPermission(), alertMsg);
        }

        platform.getPluginLogger().info("Blocked " + name + " (" + ip + ") - " +
                resolveDetectionLabel(response) + " [Score: " + response.riskScore() + "]");
    }

    public boolean shouldBlock(@NotNull VpnResponse response) {
        if (response.countryCode() != null) {
            for (String country : settings.getWhitelistedCountries()) {
                if (country.equalsIgnoreCase(response.countryCode())) {
                    return false;
                }
            }
        }

        if (response.riskScore() >= settings.getRiskScoreThreshold()) {
            return true;
        }

        for (DetectionType type : settings.getBlockedTypes()) {
            if (type.isBlocked(response)) {
                return true;
            }
        }

        return false;
    }

    private @NotNull String resolveDetectionLabel(@NotNull VpnResponse response) {
        if (response.vpn()) return "VPN";
        if (response.proxy()) return "Proxy";
        if (response.tor()) return "Tor";
        if (response.datacenter()) return "Datacenter";
        return "High Risk Score";
    }

    public void alertAndLog(@Nullable UUID uuid, @NotNull String name, @NotNull String ip, @NotNull VpnResponse response) {
        IpCheckEvent event = new IpCheckEvent(ip, uuid, name, response);
        EventBus.get().fire(event);

        if (settings.isAlertsEnabled()) {
            String alertMsg = messages.format(messages.getAlertMessage(), response).replace("{player}", name);
            platform.broadcastPermission(settings.getAlertPermission(), alertMsg);
        }

        platform.getPluginLogger().info("Blocked " + name + " (" + ip + ") - " + resolveDetectionLabel(response) + " [Score: " + response.riskScore() + "]");
    }

    public void reload(@NotNull PluginConfig mainConfig, @NotNull PluginConfig messagesConfig) {
        mainConfig.reload();
        messagesConfig.reload();
        settings.reload();
        messages.reload();
        cache.rebuild(settings);
        whitelist.load();
    }

    public void shutdown() {
        AntiVpnProvider.unregister();
        EventBus.get().clear();
        httpClient.shutdown();
        cache.clear();
    }

    public @NotNull Settings getSettings() { return settings; }
    public @NotNull Messages getMessages() { return messages; }
    public @NotNull VpnCache getCache() { return cache; }
    public @NotNull WhitelistManager getWhitelist() { return whitelist; }
    public @NotNull Platform getPlatform() { return platform; }
}
