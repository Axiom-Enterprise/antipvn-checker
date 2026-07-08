package com.axiom.antivpn.common.config;

import com.axiom.antivpn.api.model.VpnResponse;
import com.axiom.antivpn.common.color.ColorParser;
import org.jetbrains.annotations.NotNull;

public final class Messages {

    private final @NotNull PluginConfig config;

    private String prefix;
    private String kickMessage;
    private String alertMessage;
    private String whitelistAdd;
    private String whitelistRemove;
    private String whitelistAlready;
    private String whitelistNotFound;
    private String whitelistListEmpty;
    private String whitelistListIpsHeader;
    private String whitelistListIpEntry;
    private String whitelistListPlayersHeader;
    private String whitelistListPlayerEntry;
    private String reloadSuccess;
    private String noPermission;
    private String playerNotFound;
    private String invalidIp;
    private String checkResult;
    private String checkSafe;
    private String checkBlocked;
    private String cacheCleared;
    private String statusOnline;
    private String statusOffline;
    private String usageHelp;
    private String apiKeyMissing;

    public Messages(@NotNull PluginConfig config) {
        this.config = config;
        reload();
    }

    public void reload() {
        this.prefix = raw("prefix", "&#7C3AED&lAxiom &8» &7");
        this.kickMessage = raw("kick-message", "&c&lVPN Detected\n\n&7Your connection has been blocked.\n&7Disable your VPN/Proxy and reconnect.\n\n&8Risk Score: {risk_score} | {detection_type}");
        this.alertMessage = raw("alert-message", "{prefix}&e{player} &7was blocked &8(&c{detection_type}&8) &7IP: &f{ip} &7Score: &c{risk_score}");
        this.whitelistAdd = raw("whitelist-add", "{prefix}&a{target} &7has been whitelisted.");
        this.whitelistRemove = raw("whitelist-remove", "{prefix}&c{target} &7has been removed from the whitelist.");
        this.whitelistAlready = raw("whitelist-already", "{prefix}&e{target} &7is already whitelisted.");
        this.whitelistNotFound = raw("whitelist-not-found", "{prefix}&c{target} &7is not whitelisted.");
        this.whitelistListEmpty = raw("whitelist-list-empty", "{prefix}&7Whitelist is empty.");
        this.whitelistListIpsHeader = raw("whitelist-list-ips-header", "{prefix}&7Whitelisted IPs (&f{count}&7):");
        this.whitelistListIpEntry = raw("whitelist-list-ip-entry", "&8 - &f{ip}");
        this.whitelistListPlayersHeader = raw("whitelist-list-players-header", "{prefix}&7Whitelisted players (&f{count}&7):");
        this.whitelistListPlayerEntry = raw("whitelist-list-player-entry", "&8 - &f{name} &7({uuid})");
        this.reloadSuccess = raw("reload-success", "{prefix}&aConfiguration reloaded successfully.");
        this.noPermission = raw("no-permission", "{prefix}&cYou do not have permission to use this command.");
        this.playerNotFound = raw("player-not-found", "{prefix}&cPlayer not found.");
        this.invalidIp = raw("invalid-ip", "{prefix}&cInvalid IP address.");
        this.checkResult = raw("check-result", "{prefix}&7IP: &f{ip} &8| &7VPN: {vpn_status} &8| &7Proxy: {proxy_status} &8| &7Score: &e{risk_score} &8| &7ISP: &f{isp} &8| &7Country: &f{country}");
        this.checkSafe = raw("check-safe", "&a&lSAFE");
        this.checkBlocked = raw("check-blocked", "&c&lBLOCKED");
        this.cacheCleared = raw("cache-cleared", "{prefix}&aCache has been cleared. &7({size} entries removed)");
        this.statusOnline = raw("status-online", "{prefix}&aAPI Status: &2ONLINE &8| &7Latency: &a{latency}ms");
        this.statusOffline = raw("status-offline", "{prefix}&cAPI Status: &4OFFLINE");
        this.usageHelp = raw("usage-help", "{prefix}&eUsage:\n&7/vpn check <ip> &8- &7Check an IP\n&7/vpn whitelist add <ip|player> &8- &7Whitelist\n&7/vpn whitelist remove <ip|player> &8- &7Remove whitelist\n&7/vpn whitelist list &8- &7List whitelist\n&7/vpn status &8- &7API status\n&7/vpn cache clear &8- &7Clear cache\n&7/vpn reload &8- &7Reload config");
        this.apiKeyMissing = raw("api-key-missing", "{prefix}&cAPI key is not configured! Set it in config.yml");
    }

    private @NotNull String raw(@NotNull String path, @NotNull String def) {
        return config.getString(path, def);
    }

    public @NotNull String format(@NotNull String template, @NotNull VpnResponse response) {
        String result = template
                .replace("{prefix}", prefix)
                .replace("{ip}", response.ip())
                .replace("{risk_score}", String.valueOf(response.riskScore()))
                .replace("{isp}", response.isp() != null ? response.isp() : "Unknown")
                .replace("{country}", response.country() != null ? response.country() : "Unknown")
                .replace("{country_code}", response.countryCode() != null ? response.countryCode() : "??")
                .replace("{city}", response.city() != null ? response.city() : "Unknown")
                .replace("{region}", response.region() != null ? response.region() : "Unknown")
                .replace("{asn}", response.asn() != null ? response.asn() : "Unknown")
                .replace("{org}", response.org() != null ? response.org() : "Unknown")
                .replace("{vpn_status}", response.vpn() ? checkBlocked : checkSafe)
                .replace("{proxy_status}", response.proxy() ? checkBlocked : checkSafe)
                .replace("{tor_status}", response.tor() ? checkBlocked : checkSafe)
                .replace("{datacenter_status}", response.datacenter() ? checkBlocked : checkSafe)
                .replace("{detection_type}", resolveDetectionType(response));
        return ColorParser.parse(result);
    }

    public @NotNull String format(@NotNull String template) {
        return ColorParser.parse(template.replace("{prefix}", prefix));
    }

    public @NotNull String formatWith(@NotNull String template, @NotNull String key, @NotNull String value) {
        return ColorParser.parse(template.replace("{prefix}", prefix).replace(key, value));
    }

    private @NotNull String resolveDetectionType(@NotNull VpnResponse response) {
        if (response.vpn()) return "VPN";
        if (response.proxy()) return "Proxy";
        if (response.tor()) return "Tor";
        if (response.datacenter()) return "Datacenter";
        return "Clean";
    }

    public @NotNull String getPrefix() { return prefix; }
    public @NotNull String getKickMessage() { return kickMessage; }
    public @NotNull String getAlertMessage() { return alertMessage; }
    public @NotNull String getWhitelistAdd() { return whitelistAdd; }
    public @NotNull String getWhitelistRemove() { return whitelistRemove; }
    public @NotNull String getWhitelistAlready() { return whitelistAlready; }
    public @NotNull String getWhitelistNotFound() { return whitelistNotFound; }
    public @NotNull String getWhitelistListEmpty() { return whitelistListEmpty; }
    public @NotNull String getWhitelistListIpsHeader() { return whitelistListIpsHeader; }
    public @NotNull String getWhitelistListIpEntry() { return whitelistListIpEntry; }
    public @NotNull String getWhitelistListPlayersHeader() { return whitelistListPlayersHeader; }
    public @NotNull String getWhitelistListPlayerEntry() { return whitelistListPlayerEntry; }
    public @NotNull String getReloadSuccess() { return reloadSuccess; }
    public @NotNull String getNoPermission() { return noPermission; }
    public @NotNull String getPlayerNotFound() { return playerNotFound; }
    public @NotNull String getInvalidIp() { return invalidIp; }
    public @NotNull String getCheckResult() { return checkResult; }
    public @NotNull String getCacheCleared() { return cacheCleared; }
    public @NotNull String getStatusOnline() { return statusOnline; }
    public @NotNull String getStatusOffline() { return statusOffline; }
    public @NotNull String getUsageHelp() { return usageHelp; }
    public @NotNull String getApiKeyMissing() { return apiKeyMissing; }
}
