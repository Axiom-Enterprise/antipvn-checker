package com.axiom.antivpn.common.webhook;

import com.axiom.antivpn.api.model.VpnResponse;
import com.axiom.antivpn.common.config.Settings;
import com.axiom.antivpn.common.policy.PolicyDecision;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class DiscordWebhookNotifier {
    private final @NotNull HttpClient client;
    private final @NotNull Logger logger;
    private volatile URI endpoint;
    private volatile IpDisplay ipDisplay = IpDisplay.MASKED;

    public DiscordWebhookNotifier(@NotNull Settings settings, @NotNull HttpClient client, @NotNull Logger logger) {
        this.client = client;
        this.logger = logger;
        reload(settings);
    }

    public void reload(@NotNull Settings settings) {
        endpoint = null;
        ipDisplay = settings.getWebhookIpDisplay();
        if (!settings.isWebhookEnabled() || settings.getWebhookUrl().isBlank()) return;
        try {
            endpoint = WebhookUrlPolicy.validate(settings.getWebhookUrl(), settings.isWebhookAllowCustomHosts());
        } catch (IllegalArgumentException ignored) {
            logger.warning("Discord webhook disabled: invalid endpoint configuration");
        }
    }

    public @NotNull CompletableFuture<Void> notify(@NotNull String player, @NotNull String ip,
                                                    @NotNull VpnResponse response,
                                                    @NotNull PolicyDecision decision) {
        URI target = endpoint;
        if (target == null || !decision.matched()) return CompletableFuture.completedFuture(null);
        JsonObject embed = new JsonObject();
        embed.addProperty("title", "AxiomAntiVPN detection");
        embed.addProperty("color", 0x7C3AED);
        JsonArray fields = new JsonArray();
        field(fields, "Player", safe(player), true);
        field(fields, "IP", ipDisplay.render(ip), true);
        field(fields, "Action", decision.action().name(), true);
        field(fields, "Detection", decision.reason(), true);
        field(fields, "Risk score", Integer.toString(response.riskScore()), true);
        field(fields, "Country", response.countryCode() == null ? "Unknown" : response.countryCode(), true);
        embed.add("fields", fields);
        JsonObject body = new JsonObject();
        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        body.add("embeds", embeds);
        HttpRequest request = HttpRequest.newBuilder(target)
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.discarding()).handle((result, error) -> {
            if (error != null) logger.warning("Discord webhook delivery failed: " + error.getClass().getSimpleName());
            else if (result.statusCode() < 200 || result.statusCode() >= 300)
                logger.warning("Discord webhook delivery failed with status " + result.statusCode());
            return null;
        });
    }

    private static void field(JsonArray fields, String name, String value, boolean inline) {
        JsonObject field = new JsonObject();
        field.addProperty("name", name);
        field.addProperty("value", value);
        field.addProperty("inline", inline);
        fields.add(field);
    }

    private static String safe(String value) {
        String cleaned = value.replace('\r', ' ').replace('\n', ' ').strip();
        return cleaned.length() > 128 ? cleaned.substring(0, 128) : cleaned;
    }
}
