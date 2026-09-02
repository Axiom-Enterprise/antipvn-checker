package com.axiom.antivpn.common;

import com.axiom.antivpn.api.AntiVpnAPI;
import com.axiom.antivpn.api.AntiVpnProvider;
import com.axiom.antivpn.api.event.EventBus;
import com.axiom.antivpn.api.event.IpCheckEvent;
import com.axiom.antivpn.api.model.ApiStatus;
import com.axiom.antivpn.api.model.VpnResponse;
import com.axiom.antivpn.common.cache.VpnCache;
import com.axiom.antivpn.common.check.WhitelistManager;
import com.axiom.antivpn.common.check.WhitelistStorage;
import com.axiom.antivpn.common.config.Messages;
import com.axiom.antivpn.common.config.PluginConfig;
import com.axiom.antivpn.common.config.Settings;
import com.axiom.antivpn.common.http.HttpClient;
import com.axiom.antivpn.common.http.ResponseParser;
import com.axiom.antivpn.common.platform.Platform;
import com.axiom.antivpn.common.network.NetworkDecision;
import com.axiom.antivpn.common.policy.CommandTemplate;
import com.axiom.antivpn.common.policy.DetectionPolicy;
import com.axiom.antivpn.common.policy.EnforcementAction;
import com.axiom.antivpn.common.policy.PolicyDecision;
import com.axiom.antivpn.common.telemetry.CheckRecord;
import com.axiom.antivpn.common.telemetry.StatsSnapshot;
import com.axiom.antivpn.common.telemetry.TelemetryStorage;
import com.axiom.antivpn.common.telemetry.TelemetryFormatter;
import com.axiom.antivpn.common.webhook.DiscordWebhookNotifier;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class AntiVpnEngine implements AntiVpnAPI {

    private final @NotNull Platform platform;
    private final @NotNull Settings settings;
    private final @NotNull Messages messages;
    private final @NotNull VpnCache cache;
    private final @NotNull WhitelistStorage whitelistStorage;
    private final @NotNull WhitelistManager whitelist;
    private final @NotNull HttpClient httpClient;
    private final @NotNull DetectionPolicy policy;
    private final @NotNull TelemetryStorage telemetry;
    private final @NotNull DiscordWebhookNotifier webhook;
    private final @NotNull Cache<UUID, VpnResponse> lastResults;
    private final @NotNull TelemetryFormatter telemetryFormatter = new TelemetryFormatter();

    public AntiVpnEngine(@NotNull Platform platform, @NotNull PluginConfig mainConfig, @NotNull PluginConfig messagesConfig) {
        this.platform = platform;
        this.settings = new Settings(mainConfig);
        this.messages = new Messages(messagesConfig);
        this.cache = new VpnCache(settings);
        this.whitelistStorage = new WhitelistStorage(platform.getDataFolder(), platform.getPluginLogger());
        migrateLegacyWhitelist(mainConfig, whitelistStorage);
        this.whitelist = new WhitelistManager(whitelistStorage);
        this.httpClient = new HttpClient(settings, platform.getAsyncExecutor(), platform.getPluginLogger());
        this.policy = new DetectionPolicy(settings);
        this.telemetry = new TelemetryStorage(platform.getDataFolder(), platform.getPluginLogger(), settings.getHistoryMaxRows());
        this.telemetry.purgeOlderThan(java.time.Duration.ofDays(settings.getHistoryRetentionDays()));
        this.webhook = new DiscordWebhookNotifier(settings, java.net.http.HttpClient.newBuilder()
                .executor(platform.getAsyncExecutor()).connectTimeout(java.time.Duration.ofSeconds(5)).build(), platform.getPluginLogger());
        this.lastResults = Caffeine.newBuilder().maximumSize(10000).expireAfterWrite(30, TimeUnit.MINUTES).build();

        AntiVpnProvider.register(this);
    }

    private static void migrateLegacyWhitelist(@NotNull PluginConfig mainConfig, @NotNull WhitelistStorage storage) {
        if (!mainConfig.contains("whitelist.ips") && !mainConfig.contains("whitelist.players")) {
            return;
        }

        for (String ip : mainConfig.getStringList("whitelist.ips")) {
            storage.addIp(ip.toLowerCase(Locale.ROOT));
        }

        List<String> names = mainConfig.getStringList("whitelist.player-names");
        List<UUID> uuids = new ArrayList<>();
        for (String raw : mainConfig.getStringList("whitelist.players")) {
            try {
                uuids.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
                storage.addIp(raw.toLowerCase(Locale.ROOT));
            }
        }
        for (int i = 0; i < uuids.size(); i++) {
            String name = i < names.size() ? names.get(i) : null;
            storage.addPlayer(uuids.get(i), name);
        }

        mainConfig.set("whitelist.ips", null);
        mainConfig.set("whitelist.players", null);
        mainConfig.set("whitelist.player-names", null);
        mainConfig.save();
    }

    @Override
    public @NotNull CompletableFuture<VpnResponse> checkIp(@NotNull String ip) {
        VpnResponse cached = cache.get(ip);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached.withCached(true));
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
                .thenAccept(response -> {
                    PolicyDecision decision = processCheck(uuid, name, ip, response);
                    if (decision.action() == EnforcementAction.KICK) {
                        platform.kickPlayer(uuid, messages.format(messages.getKickMessage(), response));
                    }
                })
                .exceptionally(ex -> {
                    platform.getPluginLogger().log(Level.WARNING, "Failed to check IP for " + name + ": " + ex.getClass().getSimpleName());
                    recordFailure(uuid, name, ip);
                    if (settings.isBlockOnApiFailure()) {
                        platform.kickPlayer(uuid, messages.format(messages.getKickMessage()));
                    }
                    return null;
                });
    }

    public @NotNull PolicyDecision processCheck(@Nullable UUID uuid, @NotNull String name,
                                                @NotNull String ip, @NotNull VpnResponse response) {
        IpCheckEvent event = new IpCheckEvent(ip, uuid, name, response);
        EventBus.get().fire(event);
        if (event.isCancelled()) {
            return PolicyDecision.allow("event-cancelled");
        }
        PolicyDecision decision = policy.evaluate(response);
        if (uuid != null) lastResults.put(uuid, response);
        if (settings.isHistoryEnabled()) {
            telemetry.record(new CheckRecord(System.currentTimeMillis(), uuid, name, ip, response.countryCode(),
                    decision.reason(), response.riskScore(), decision.action(), decision.matched(), response.cached(), false));
        }
        if (!decision.matched()) return decision;
        alert(name, response);
        if (decision.action() == EnforcementAction.COMMAND) {
            for (String template : decision.commands()) {
                try { platform.dispatchConsoleCommand(CommandTemplate.render(template, name, ip, response.riskScore(), decision.reason())); }
                catch (IllegalArgumentException e) { platform.getPluginLogger().warning("Configured AntiVPN command was rejected"); }
            }
        }
        webhook.notify(name, ip, response, decision);
        platform.getPluginLogger().info("Detected " + name + " - " + decision.reason() + " [Score: " + response.riskScore() + ", Action: " + decision.action() + "]");
        return decision;
    }

    public boolean shouldBlock(@NotNull VpnResponse response) {
        return policy.evaluate(response).action() == EnforcementAction.KICK;
    }

    private @NotNull String resolveDetectionLabel(@NotNull VpnResponse response) {
        if (response.vpn()) return "VPN";
        if (response.proxy()) return "Proxy";
        if (response.tor()) return "Tor";
        if (response.datacenter()) return "Datacenter";
        return "High Risk Score";
    }

    public void alertAndLog(@Nullable UUID uuid, @NotNull String name, @NotNull String ip, @NotNull VpnResponse response) {
        processCheck(uuid, name, ip, response);
    }

    private void alert(@NotNull String name, @NotNull VpnResponse response) {
        if (settings.isAlertsEnabled()) {
            String alertMsg = messages.format(messages.getAlertMessage(), response).replace("{player}", name);
            platform.broadcastPermission(settings.getAlertPermission(), alertMsg);
        }
    }

    public void recordFailure(@Nullable UUID uuid, @NotNull String name, @NotNull String ip) {
        if (settings.isHistoryEnabled()) telemetry.record(new CheckRecord(System.currentTimeMillis(), uuid, name, ip, null,
                "API_FAILURE", 0, settings.isBlockOnApiFailure() ? EnforcementAction.KICK : EnforcementAction.ALLOW,
                settings.isBlockOnApiFailure(), false, true));
    }

    public void acceptNetworkDecision(@NotNull String playerName, @NotNull NetworkDecision decision) {
        if (settings.isHistoryEnabled()) telemetry.record(new CheckRecord(System.currentTimeMillis(), decision.playerUuid(), playerName,
                decision.ip(), null, decision.reason(), decision.riskScore(), decision.action(),
                decision.action() != EnforcementAction.ALLOW, false, false));
    }

    public void reload(@NotNull PluginConfig mainConfig, @NotNull PluginConfig messagesConfig) {
        mainConfig.reload();
        messagesConfig.reload();
        settings.reload();
        messages.reload();
        cache.rebuild(settings);
        whitelist.load();
        webhook.reload(settings);
    }

    public void shutdown() {
        AntiVpnProvider.unregister();
        EventBus.get().clear();
        httpClient.shutdown();
        cache.clear();
        whitelistStorage.close();
        telemetry.close();
    }

    public @NotNull Settings getSettings() { return settings; }
    public @NotNull Messages getMessages() { return messages; }
    public @NotNull VpnCache getCache() { return cache; }
    public @NotNull WhitelistManager getWhitelist() { return whitelist; }
    public @NotNull Platform getPlatform() { return platform; }
    public @NotNull StatsSnapshot getStats() { return telemetry.stats(); }
    public @NotNull List<CheckRecord> getHistory(@NotNull String player, int limit) { return telemetry.historyByPlayer(player, limit); }
    public @Nullable VpnResponse getLastResult(@NotNull UUID uuid) { return lastResults.getIfPresent(uuid); }
    public @NotNull PolicyDecision evaluate(@NotNull VpnResponse response) { return policy.evaluate(response); }
    public @NotNull TelemetryFormatter getTelemetryFormatter() { return telemetryFormatter; }
}
