package com.axiom.antivpn.common.config;

import com.axiom.antivpn.api.model.DetectionType;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
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

    private long cacheTtlSeconds;
    private long cacheMaxSize;

    private List<String> whitelistedIps;
    private List<String> whitelistedPlayers;
    private List<String> whitelistedCountries;

    private boolean alertsEnabled;
    private String alertPermission;

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

        this.cacheTtlSeconds = config.getLong("cache.ttl-seconds", 600);
        this.cacheMaxSize = config.getLong("cache.max-size", 10000);

        this.whitelistedIps = config.getStringList("whitelist.ips");
        this.whitelistedPlayers = config.getStringList("whitelist.players");
        this.whitelistedCountries = config.getStringList("whitelist.countries");

        this.alertsEnabled = config.getBoolean("alerts.enabled", true);
        this.alertPermission = config.getString("alerts.permission", "antivpn.alerts");

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

    public long getCacheTtlSeconds() { return cacheTtlSeconds; }
    public long getCacheMaxSize() { return cacheMaxSize; }

    public @NotNull List<String> getWhitelistedIps() { return whitelistedIps; }
    public @NotNull List<String> getWhitelistedPlayers() { return whitelistedPlayers; }
    public @NotNull List<String> getWhitelistedCountries() { return whitelistedCountries; }

    public boolean isAlertsEnabled() { return alertsEnabled; }
    public @NotNull String getAlertPermission() { return alertPermission; }

    public boolean isCheckOnLogin() { return checkOnLogin; }
    public boolean isCheckOnServerSwitch() { return checkOnServerSwitch; }
    public boolean isAsyncCheck() { return asyncCheck; }
}
