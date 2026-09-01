package com.axiom.antivpn.common.webhook;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

public final class WebhookUrlPolicy {
    private static final Set<String> DISCORD_HOSTS = Set.of(
            "discord.com", "discordapp.com", "canary.discord.com", "ptb.discord.com");

    private WebhookUrlPolicy() {
    }

    public static @NotNull URI validate(@NotNull String raw, boolean allowCustomHosts) {
        if (raw.length() > 2048) throw new IllegalArgumentException("Webhook URL is too long");
        URI uri = URI.create(raw);
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Webhook URL must use HTTPS without user-info");
        }
        if (!allowCustomHosts) {
            String normalized = host.toLowerCase(Locale.ROOT);
            if (!DISCORD_HOSTS.contains(normalized) || !uri.getPath().startsWith("/api/webhooks/")) {
                throw new IllegalArgumentException("Webhook URL is not an allowed Discord endpoint");
            }
        }
        return uri;
    }
}
