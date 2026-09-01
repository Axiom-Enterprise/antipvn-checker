package com.axiom.antivpn.common.config;

import com.axiom.antivpn.api.model.DetectionType;
import com.axiom.antivpn.common.policy.EnforcementAction;
import com.axiom.antivpn.common.webhook.IpDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.EnumMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Settings {

    private final @NotNull PluginConfig config;

    private String apiKey;
    private String apiBaseUrl;
    private int apiTimeoutMs;
    private int apiMaxRetries;

    private Set<DetectionType> blockedTypes;
    private int riskScoreThreshold;
    private boolean blockOnApiFailure;
    private boolean monitorMode;
    private EnforcementAction defaultAction;
    private Map<DetectionType, EnforcementAction> typeActions;
    private NavigableMap<Integer, EnforcementAction> riskActions;
    private List<String> actionCommands;

    private long cacheTtlSeconds;
    private long cacheMaxSize;

    private List<String> whitelistedCountries;

    private boolean alertsEnabled;
    private String alertPermission;
    private boolean webhookEnabled;
    private String webhookUrl;
    private IpDisplay webhookIpDisplay;
    private boolean webhookAllowCustomHosts;

    private boolean historyEnabled;
    private int historyRetentionDays;
    private long historyMaxRows;

    private String networkMode;
    private String networkSecret;

    private boolean checkOnLogin;
    private boolean checkOnServerSwitch;
    private boolean asyncCheck;

    public Settings(@NotNull PluginConfig config) {
        this.config = config;
        reload();
    }

    public void reload() {
        this.apiKey = config.getString("api.key", "");
        this.apiBaseUrl = config.getString("api.base-url", "https://antivpn.mathsanalysis.com/api");
        this.apiTimeoutMs = config.getInt("api.timeout-ms", 5000);
        this.apiMaxRetries = config.getInt("api.max-retries", 2);

        this.blockedTypes = EnumSet.noneOf(DetectionType.class);
        for (String type : config.getStringList("detection.blocked-types")) {
            try {
                blockedTypes.add(DetectionType.valueOf(type.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        this.riskScoreThreshold = config.getInt("detection.risk-score-threshold", 75);
        this.blockOnApiFailure = config.getBoolean("detection.block-on-api-failure", false);
        this.monitorMode = "MONITOR".equalsIgnoreCase(config.getString("mode", "ENFORCE"));
        this.defaultAction = action(config.getString("actions.default", "KICK"), EnforcementAction.KICK);
        this.typeActions = new EnumMap<>(DetectionType.class);
        for (Map.Entry<String, Object> entry : config.getSection("actions.by-type").entrySet()) {
            try {
                DetectionType type = DetectionType.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
                typeActions.put(type, action(String.valueOf(entry.getValue()), defaultAction));
            } catch (IllegalArgumentException ignored) {
            }
        }
        this.riskActions = new TreeMap<>();
        for (Map.Entry<String, Object> entry : config.getSection("actions.by-risk-score").entrySet()) {
            try {
                int threshold = Integer.parseInt(entry.getKey());
                if (threshold >= 0 && threshold <= 100) riskActions.put(threshold, action(String.valueOf(entry.getValue()), defaultAction));
            } catch (NumberFormatException ignored) {
            }
        }
        this.actionCommands = config.getStringList("actions.commands");

        this.cacheTtlSeconds = config.getLong("cache.ttl-seconds", 600);
        this.cacheMaxSize = config.getLong("cache.max-size", 10000);

        this.whitelistedCountries = config.getStringList("whitelist.countries");

        this.alertsEnabled = config.getBoolean("alerts.enabled", true);
        this.alertPermission = config.getString("alerts.permission", "antivpn.alerts");
        this.webhookEnabled = config.getBoolean("alerts.discord.enabled", false);
        this.webhookUrl = config.getString("alerts.discord.webhook-url", "");
        this.webhookIpDisplay = ipDisplay(config.getString("alerts.discord.ip-display", "MASKED"));
        this.webhookAllowCustomHosts = config.getBoolean("alerts.discord.allow-custom-hosts", false);

        this.historyEnabled = config.getBoolean("history.enabled", true);
        this.historyRetentionDays = clamp(config.getInt("history.retention-days", 30), 1, 3650);
        this.historyMaxRows = Math.max(1000, config.getLong("history.max-rows", 100000));

        this.networkMode = config.getString("network.mode", "STANDALONE").toUpperCase(Locale.ROOT);
        if (!Set.of("STANDALONE", "PROXY", "BACKEND").contains(networkMode)) networkMode = "STANDALONE";
        this.networkSecret = config.getString("network.channel-secret", "");
        if (!networkMode.equals("STANDALONE") && networkSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            networkMode = "STANDALONE";
        }

        this.checkOnLogin = config.getBoolean("connection.check-on-login", true);
        this.checkOnServerSwitch = config.getBoolean("connection.check-on-server-switch", false);
        this.asyncCheck = config.getBoolean("connection.async-check", true);
    }

    public @NotNull String getApiKey() { return apiKey; }
    public @NotNull String getApiBaseUrl() { return apiBaseUrl; }
    public int getApiTimeoutMs() { return apiTimeoutMs; }
    public int getApiMaxRetries() { return apiMaxRetries; }

    public @NotNull Set<DetectionType> getBlockedTypes() { return blockedTypes; }
    public int getRiskScoreThreshold() { return riskScoreThreshold; }
    public boolean isBlockOnApiFailure() { return blockOnApiFailure; }
    public boolean isMonitorMode() { return monitorMode; }
    public @NotNull EnforcementAction getDefaultAction() { return defaultAction; }
    public @NotNull Map<DetectionType, EnforcementAction> getTypeActions() { return Map.copyOf(typeActions); }
    public @NotNull NavigableMap<Integer, EnforcementAction> getRiskActions() { return new TreeMap<>(riskActions); }
    public @NotNull List<String> getActionCommands() { return List.copyOf(actionCommands); }

    public long getCacheTtlSeconds() { return cacheTtlSeconds; }
    public long getCacheMaxSize() { return cacheMaxSize; }

    public @NotNull List<String> getWhitelistedCountries() { return whitelistedCountries; }

    public boolean isAlertsEnabled() { return alertsEnabled; }
    public @NotNull String getAlertPermission() { return alertPermission; }
    public boolean isWebhookEnabled() { return webhookEnabled; }
    public @NotNull String getWebhookUrl() { return webhookUrl; }
    public @NotNull IpDisplay getWebhookIpDisplay() { return webhookIpDisplay; }
    public boolean isWebhookAllowCustomHosts() { return webhookAllowCustomHosts; }
    public boolean isHistoryEnabled() { return historyEnabled; }
    public int getHistoryRetentionDays() { return historyRetentionDays; }
    public long getHistoryMaxRows() { return historyMaxRows; }
    public @NotNull String getNetworkMode() { return networkMode; }
    public @NotNull String getNetworkSecret() { return networkSecret; }

    public boolean isCheckOnLogin() { return checkOnLogin; }
    public boolean isCheckOnServerSwitch() { return checkOnServerSwitch; }
    public boolean isAsyncCheck() { return asyncCheck; }

    private static EnforcementAction action(String raw, EnforcementAction fallback) {
        try { return EnforcementAction.valueOf(raw.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }

    private static IpDisplay ipDisplay(String raw) {
        try { return IpDisplay.valueOf(raw.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return IpDisplay.MASKED; }
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
