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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Command("vpn")
@CommandPermission("antivpn.admin")
public final class AntiVpnCommand {

    private static final HttpClient MOJANG_CLIENT = HttpClient.newHttpClient();

    private final @NotNull AntiVpnEngine engine;
    private final @NotNull ProxyServer server;

    public AntiVpnCommand(@NotNull AntiVpnEngine engine, @NotNull ProxyServer server) {
        this.engine = engine;
        this.server = server;
    }

    private void reply(@NotNull VelocityCommandActor actor, @NotNull String message) {
        actor.reply(VelocitySerializer.INSTANCE.deserialize(message));
    }

    @Command("vpn")
    public void usage(@NotNull VelocityCommandActor actor) {
        Messages messages = engine.getMessages();
        reply(actor, messages.format(messages.getUsageHelp()));
    }

    @Subcommand("check")
    public void check(@NotNull VelocityCommandActor actor, @NotNull String ip) {
        Messages messages = engine.getMessages();
        if (!IpUtil.isValidIp(ip)) {
            reply(actor, messages.format(messages.getInvalidIp()));
            return;
        }

        reply(actor, messages.format("{prefix}&7Checking IP &f" + ip + "&7..."));
        engine.checkIp(ip).thenAccept(response ->
                reply(actor, messages.format(messages.getCheckResult(), response))
        ).exceptionally(ex -> {
            reply(actor, messages.format("{prefix}&cFailed to check IP: " + ex.getMessage()));
            return null;
        });
    }

    @Subcommand("status")
    public void status(@NotNull VelocityCommandActor actor) {
        Messages messages = engine.getMessages();
        long start = System.currentTimeMillis();
        engine.getStatus().thenAccept(status -> {
            long latency = System.currentTimeMillis() - start;
            if (status.isOperational()) {
                reply(actor, messages.format(messages.getStatusOnline())
                        .replace("{latency}", String.valueOf(latency))
                        .replace("{status}", status.overallStatus().toUpperCase()));
            } else {
                reply(actor, messages.format(messages.getStatusOffline()));
            }
        }).exceptionally(ex -> {
            reply(actor, messages.format(messages.getStatusOffline()));
            return null;
        });
    }

    @Subcommand("cache clear")
    public void cacheClear(@NotNull VelocityCommandActor actor) {
        Messages messages = engine.getMessages();
        long size = engine.getCache().size();
        engine.clearCache();
        reply(actor, messages.formatWith(messages.getCacheCleared(), "{size}", String.valueOf(size)));
    }

    @Subcommand("reload")
    public void reload(@NotNull VelocityCommandActor actor) {
        Messages messages = engine.getMessages();
        VelocityPlatform platform = (VelocityPlatform) engine.getPlatform();
        VelocityConfig mainConfig = new VelocityConfig(platform.getDataFolder().resolve("config.yml"), platform.getPluginLogger());
        VelocityConfig messagesConfig = new VelocityConfig(platform.getDataFolder().resolve("messages.yml"), platform.getPluginLogger());
        engine.reload(mainConfig, messagesConfig);
        reply(actor, messages.format(messages.getReloadSuccess()));
    }

    @Subcommand("whitelist add")
    public void whitelistAdd(@NotNull VelocityCommandActor actor, @OnlinePlayerNames @NotNull String target) {
        whitelistApply(actor, "add", target);
    }

    @Subcommand("whitelist remove")
    public void whitelistRemove(@NotNull VelocityCommandActor actor, @OnlinePlayerNames @NotNull String target) {
        whitelistApply(actor, "remove", target);
    }

    @Subcommand("whitelist list")
    public void whitelistList(@NotNull VelocityCommandActor actor) {
        Messages messages = engine.getMessages();
        Set<String> ips = engine.getWhitelist().getWhitelistedIps();
        Map<UUID, String> players = engine.getWhitelist().getWhitelistedPlayers();

        if (ips.isEmpty() && players.isEmpty()) {
            reply(actor, messages.format(messages.getWhitelistListEmpty()));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(messages.getWhitelistListIpsHeader().replace("{count}", String.valueOf(ips.size())));
        for (String ip : ips) {
            sb.append('\n').append(messages.getWhitelistListIpEntry().replace("{ip}", ip));
        }
        sb.append('\n').append(messages.getWhitelistListPlayersHeader().replace("{count}", String.valueOf(players.size())));
        for (Map.Entry<UUID, String> entry : players.entrySet()) {
            String name = entry.getValue().isEmpty() ? "?" : entry.getValue();
            sb.append('\n').append(messages.getWhitelistListPlayerEntry()
                    .replace("{name}", name)
                    .replace("{uuid}", entry.getKey().toString()));
        }
        reply(actor, messages.format(sb.toString()));
    }

    private void whitelistApply(@NotNull VelocityCommandActor actor, @NotNull String action, @NotNull String target) {
        Messages messages = engine.getMessages();

        if (IpUtil.isValidIp(target)) {
            boolean changed = action.equals("add")
                    ? engine.getWhitelist().addIp(target)
                    : engine.getWhitelist().removeIp(target);
            whitelistSendResult(actor, messages, action, target, changed);
            return;
        }

        resolvePlayerUuid(target).thenAccept(uuid -> {
            if (uuid == null) {
                reply(actor, messages.format(messages.getPlayerNotFound()));
                return;
            }
            boolean changed = action.equals("add")
                    ? engine.getWhitelist().addPlayer(uuid, target)
                    : engine.getWhitelist().removePlayer(uuid, target);
            whitelistSendResult(actor, messages, action, target, changed);
        }).exceptionally(ex -> {
            reply(actor, messages.format("{prefix}&cFailed to resolve player: " + ex.getMessage()));
            return null;
        });
    }

    private void whitelistSendResult(@NotNull VelocityCommandActor actor, @NotNull Messages messages,
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
