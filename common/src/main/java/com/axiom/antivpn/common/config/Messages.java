package com.axiom.antivpn.common.config;

import com.axiom.antivpn.api.model.VpnResponse;
import com.axiom.antivpn.common.color.ColorParser;
import com.axiom.antivpn.common.policy.PolicyDecision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Messages {

    private static final String DEFAULT_PREFIX = "&#7C3AED&lAxiom &8» &7";
    private static final List<String> DEFAULT_KICK = List.of(
            "&c&lVPN Detected",
            "",
            "&7Your connection has been blocked.",
            "&7Disable your VPN/Proxy and reconnect.",
            "",
            "&8Risk Score: {risk_score} | {detection_type}");
    private static final List<String> DEFAULT_ALERT = List.of(
            "{prefix}&e{player} &7was blocked &8(&c{detection_type}&8) &7IP: &f{ip} &7Score: &c{risk_score}");

    private static final String[] FAILURE_PLACEHOLDERS = {
            "{risk_score}", "N/A", "{detection_type}", "Unavailable",
            "{ip}", "Unknown", "{isp}", "Unknown", "{country}", "Unknown", "{country_code}", "Unknown",
            "{city}", "Unknown", "{region}", "Unknown", "{timezone}", "Unknown", "{asn}", "Unknown", "{org}", "Unknown",
            "{vpn_status}", "N/A", "{proxy_status}", "N/A", "{tor_status}", "N/A", "{datacenter_status}", "N/A",
            "{residential_status}", "N/A", "{mobile_status}", "N/A"
    };

    private final @NotNull PluginConfig config;

    private String prefix;
    private String legacyPrefix;
    private String miniPrefix;
    private List<String> kickMessage;
    private List<String> alertMessage;
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
    private String playerResolveFailed;
    private String invalidIp;
    private String checkPending;
    private String checkResult;
    private String checkFailed;
    private String checkSafe;
    private String checkBlocked;
    private String checkSafeMini;
    private String checkBlockedMini;
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
        this.prefix = raw("prefix", DEFAULT_PREFIX);
        this.legacyPrefix = ColorParser.isMiniMessage(prefix) ? ColorParser.parse(prefix) : prefix;
        this.miniPrefix = ColorParser.isMiniMessage(prefix)
                ? prefix.substring(ColorParser.MINI_MESSAGE_PREFIX.length())
                : ColorParser.toMiniMessage(prefix);
        this.kickMessage = lines("kick-message", DEFAULT_KICK);
        this.alertMessage = lines("alert-message", DEFAULT_ALERT);
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
        this.playerResolveFailed = raw("player-resolve-failed", "{prefix}&cFailed to resolve player: &7{error}");
        this.invalidIp = raw("invalid-ip", "{prefix}&cInvalid IP address.");
        this.checkPending = raw("check-pending", "{prefix}&7Checking IP &f{ip}&7...");
        this.checkResult = raw("check-result", "{prefix}&7IP: &f{ip} &8| &7VPN: {vpn_status} &8| &7Proxy: {proxy_status} &8| &7Score: &e{risk_score} &8| &7ISP: &f{isp} &8| &7Country: &f{country}");
        this.checkFailed = raw("check-failed", "{prefix}&cFailed to check IP: &7{error}");
        this.checkSafe = raw("check-safe", "&a&lSAFE");
        this.checkBlocked = raw("check-blocked", "&c&lBLOCKED");
        this.checkSafeMini = ColorParser.toMiniMessage(checkSafe);
        this.checkBlockedMini = ColorParser.toMiniMessage(checkBlocked);
        this.cacheCleared = raw("cache-cleared", "{prefix}&aCache has been cleared. &7({size} entries removed)");
        this.statusOnline = raw("status-online", "{prefix}&aAPI Status: &2{status} &8| &7Latency: &a{latency}ms");
        this.statusOffline = raw("status-offline", "{prefix}&cAPI Status: &4OFFLINE");
        this.usageHelp = raw("usage-help", "{prefix}&eUsage:\n&7/vpn check <ip> &8- &7Check an IP\n&7/vpn stats &8- &7Statistics\n&7/vpn history <player> [limit] &8- &7Recent checks\n&7/vpn whitelist add <ip|player> &8- &7Whitelist\n&7/vpn whitelist remove <ip|player> &8- &7Remove whitelist\n&7/vpn whitelist list &8- &7List whitelist\n&7/vpn status &8- &7API status\n&7/vpn cache clear &8- &7Clear cache\n&7/vpn reload &8- &7Reload config");
        this.apiKeyMissing = raw("api-key-missing", "{prefix}&cAPI key is not configured! Set it in config.yml");
    }

    private @NotNull String raw(@NotNull String path, @NotNull String def) {
        return config.getString(path, def);
    }

    private @NotNull List<String> lines(@NotNull String path, @NotNull List<String> def) {
        List<String> list = config.getStringList(path);
        if (!list.isEmpty()) return List.copyOf(list);
        String single = config.getString(path, "");
        return single.isEmpty() ? def : List.of(single);
    }

    public @NotNull String formatKick(@NotNull VpnResponse response) {
        return formatLines(kickMessage, response);
    }

    public @NotNull String formatKick() {
        return formatLines(kickMessage, null, FAILURE_PLACEHOLDERS);
    }

    public @NotNull String formatAlert(@NotNull String player, @NotNull VpnResponse response, @NotNull PolicyDecision decision) {
        return formatLines(alertMessage, response,
                "{player}", player,
                "{action}", decision.action().name(),
                "{reason}", decision.reason());
    }

    public @NotNull String format(@NotNull String template) {
        return render(template, null);
    }

    public @NotNull String format(@NotNull String template, @NotNull String... pairs) {
        return render(template, null, pairs);
    }

    public @NotNull String format(@NotNull String template, @NotNull VpnResponse response) {
        return render(template, response);
    }

    public @NotNull String formatLines(@NotNull List<String> lines, @Nullable VpnResponse response, @NotNull String... pairs) {
        if (lines.size() == 1) return render(lines.get(0), response, pairs);
        StringBuilder out = new StringBuilder(lines.size() * 48);
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) out.append('\n');
            out.append(render(lines.get(i), response, pairs));
        }
        return out.toString();
    }

    private @NotNull String render(@NotNull String template, @Nullable VpnResponse response, @NotNull String... pairs) {
        boolean mini = ColorParser.isMiniMessage(template);
        String body = template.replace("{prefix}", mini ? miniPrefix : legacyPrefix);
        if (response != null) {
            body = body
                    .replace("{ip}", value(response.ip(), mini))
                    .replace("{risk_score}", Integer.toString(response.riskScore()))
                    .replace("{isp}", value(response.isp(), mini))
                    .replace("{country}", value(response.country(), mini))
                    .replace("{country_code}", value(response.countryCode(), mini))
                    .replace("{city}", value(response.city(), mini))
                    .replace("{region}", value(response.region(), mini))
                    .replace("{timezone}", value(response.timezone(), mini))
                    .replace("{asn}", value(response.asn(), mini))
                    .replace("{org}", value(response.org(), mini))
                    .replace("{vpn_status}", status(response.vpn(), mini))
                    .replace("{proxy_status}", status(response.proxy(), mini))
                    .replace("{tor_status}", status(response.tor(), mini))
                    .replace("{datacenter_status}", status(response.datacenter(), mini))
                    .replace("{residential_status}", status(response.residential(), mini))
                    .replace("{mobile_status}", status(response.mobile(), mini))
                    .replace("{detection_type}", resolveDetectionType(response));
        }
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            body = body.replace(pairs[i], value(pairs[i + 1], mini));
        }
        return ColorParser.parse(body);
    }

    private @NotNull String status(boolean flagged, boolean mini) {
        if (mini) return flagged ? checkBlockedMini : checkSafeMini;
        return flagged ? checkBlocked : checkSafe;
    }

    private static @NotNull String value(@Nullable String value, boolean mini) {
        String safe = value == null ? "Unknown" : value.replace('\r', ' ').replace('\n', ' ');
        return mini ? ColorParser.escapeMiniMessage(safe) : safe;
    }

    private static @NotNull String resolveDetectionType(@NotNull VpnResponse response) {
        if (response.vpn()) return "VPN";
        if (response.proxy()) return "Proxy";
        if (response.tor()) return "Tor";
        if (response.datacenter()) return "Datacenter";
        if (response.residential()) return "Residential";
        if (response.mobile()) return "Mobile";
        return "Clean";
    }

    public @NotNull String getPrefix() { return prefix; }
    public @NotNull List<String> getKickMessage() { return kickMessage; }
    public @NotNull List<String> getAlertMessage() { return alertMessage; }
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
    public @NotNull String getPlayerResolveFailed() { return playerResolveFailed; }
    public @NotNull String getInvalidIp() { return invalidIp; }
    public @NotNull String getCheckPending() { return checkPending; }
    public @NotNull String getCheckResult() { return checkResult; }
    public @NotNull String getCheckFailed() { return checkFailed; }
    public @NotNull String getCacheCleared() { return cacheCleared; }
    public @NotNull String getStatusOnline() { return statusOnline; }
    public @NotNull String getStatusOffline() { return statusOffline; }
    public @NotNull String getUsageHelp() { return usageHelp; }
    public @NotNull String getApiKeyMissing() { return apiKeyMissing; }
}
