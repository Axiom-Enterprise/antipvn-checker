package com.axiom.antivpn.velocity;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.command.OnlinePlayerNames;
import com.axiom.antivpn.common.config.Messages;
import com.axiom.antivpn.common.util.IpUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.velocity.actor.VelocityCommandActor;
import revxrsal.commands.velocity.annotation.CommandPermission;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Command("vpn-whitelist")
@CommandPermission("antivpn.admin")
public final class VpnWhitelistCommand {

    private static final HttpClient MOJANG_CLIENT = HttpClient.newHttpClient();

    private final @NotNull AntiVpnEngine engine;
    private final @NotNull ProxyServer server;

    public VpnWhitelistCommand(@NotNull AntiVpnEngine engine, @NotNull ProxyServer server) {
        this.engine = engine;
        this.server = server;
    }

    @Subcommand("add")
    public void add(@NotNull VelocityCommandActor actor, @OnlinePlayerNames @NotNull String target) {
        apply(actor, "add", target);
    }

    @Subcommand("remove")
    public void remove(@NotNull VelocityCommandActor actor, @OnlinePlayerNames @NotNull String target) {
        apply(actor, "remove", target);
    }

    private void reply(@NotNull VelocityCommandActor actor, @NotNull String message) {
        actor.reply(VelocitySerializer.INSTANCE.deserialize(message));
    }

    private void apply(@NotNull VelocityCommandActor actor, @NotNull String action, @NotNull String target) {
        Messages messages = engine.getMessages();

        if (IpUtil.isValidIp(target)) {
            boolean changed = action.equals("add")
                    ? engine.getWhitelist().addIp(target)
                    : engine.getWhitelist().removeIp(target);
            sendResult(actor, messages, action, target, changed);
            return;
        }

        resolvePlayerUuid(target).thenAccept(uuid -> {
            if (uuid == null) {
                reply(actor, messages.format(messages.getPlayerNotFound()));
                return;
            }
            boolean changed = action.equals("add")
                    ? engine.getWhitelist().addPlayer(uuid)
                    : engine.getWhitelist().removePlayer(uuid);
            sendResult(actor, messages, action, target, changed);
        }).exceptionally(ex -> {
            reply(actor, messages.format("{prefix}&cFailed to resolve player: " + ex.getMessage()));
            return null;
        });
    }

    private void sendResult(@NotNull VelocityCommandActor actor, @NotNull Messages messages,
                             @NotNull String action, @NotNull String target, boolean changed) {
        if (action.equals("add")) {
            reply(actor, messages.formatWith(changed ? messages.getWhitelistAdd() : messages.getWhitelistAlready(), "{target}", target));
        } else {
            reply(actor, messages.formatWith(changed ? messages.getWhitelistRemove() : messages.getWhitelistNotFound(), "{target}", target));
        }
    }

    private @NotNull CompletableFuture<UUID> resolvePlayerUuid(@NotNull String name) {
        var online = server.getPlayer(name);
        if (online.isPresent()) {
            return CompletableFuture.completedFuture(online.get().getUniqueId());
        }

        if (!server.getConfiguration().isOnlineMode()) {
            return CompletableFuture.completedFuture(
                    UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/"
                        + URLEncoder.encode(name, StandardCharsets.UTF_8)))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        return MOJANG_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200 || response.body().isBlank()) {
                        return null;
                    }
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    String id = json.get("id").getAsString();
                    return UUID.fromString(id.replaceFirst(
                            "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                });
    }
}
