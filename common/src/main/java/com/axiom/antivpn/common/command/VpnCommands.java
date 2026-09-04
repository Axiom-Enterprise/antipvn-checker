package com.axiom.antivpn.common.command;

import com.axiom.antivpn.api.model.ApiStatus;
import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.config.Messages;
import com.axiom.antivpn.common.util.IpUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Function;

public final class VpnCommands {

    public static final String ADMIN_PERMISSION = "antivpn.admin";
    public static final int DEFAULT_HISTORY_LIMIT = 10;

    private final @NotNull AntiVpnEngine engine;
    private final @NotNull Function<String, CompletableFuture<@Nullable UUID>> playerResolver;

    public VpnCommands(@NotNull AntiVpnEngine engine, @NotNull Function<String, CompletableFuture<@Nullable UUID>> playerResolver) {
        this.engine = engine;
        this.playerResolver = playerResolver;
    }

    public void usage(@NotNull Consumer<String> reply) {
        reply.accept(messages().format(messages().getUsageHelp()));
    }

    public void noPermission(@NotNull Consumer<String> reply) {
        reply.accept(messages().format(messages().getNoPermission()));
    }

    public void check(@NotNull Consumer<String> reply, @NotNull String ip) {
        Messages messages = messages();
        if (!IpUtil.isValidIp(ip)) {
            reply.accept(messages.format(messages.getInvalidIp()));
            return;
        }
        reply.accept(messages.format(messages.getCheckPending(), "{ip}", ip));
        engine.checkIp(ip).whenComplete((response, error) -> {
            if (error != null) {
                reply.accept(messages.format(messages.getCheckFailed(), "{error}", describe(error)));
            } else {
                reply.accept(messages.format(messages.getCheckResult(), response));
            }
        });
    }

    public void status(@NotNull Consumer<String> reply) {
        Messages messages = messages();
        long started = System.nanoTime();
        engine.getStatus().whenComplete((status, error) -> {
            if (error != null || !status.isOperational()) {
                reply.accept(messages.format(messages.getStatusOffline()));
                return;
            }
            long latencyMs = (System.nanoTime() - started) / 1_000_000L;
            reply.accept(messages.format(messages.getStatusOnline(),
                    "{latency}", Long.toString(latencyMs),
                    "{status}", statusLabel(status)));
        });
    }

    public void cacheClear(@NotNull Consumer<String> reply) {
        long size = engine.getCache().size();
        engine.clearCache();
        reply.accept(messages().format(messages().getCacheCleared(), "{size}", Long.toString(size)));
    }

    public void stats(@NotNull Consumer<String> reply) {
        reply.accept(messages().format(engine.getTelemetryFormatter().formatStats(engine.getStats())));
    }

    public void history(@NotNull Consumer<String> reply, @NotNull String player, int limit) {
        reply.accept(messages().format(engine.getTelemetryFormatter().formatHistory(engine.getHistory(player, limit))));
    }

    public void reload(@NotNull Consumer<String> reply) {
        engine.reload();
        reply.accept(messages().format(messages().getReloadSuccess()));
    }

    public void whitelistAdd(@NotNull Consumer<String> reply, @NotNull String target) {
        whitelistApply(reply, target, true);
    }

    public void whitelistRemove(@NotNull Consumer<String> reply, @NotNull String target) {
        whitelistApply(reply, target, false);
    }

    public void whitelistList(@NotNull Consumer<String> reply) {
        Messages messages = messages();
        Set<String> ips = engine.getWhitelist().getWhitelistedIps();
        Map<UUID, String> players = engine.getWhitelist().getWhitelistedPlayers();
        if (ips.isEmpty() && players.isEmpty()) {
            reply.accept(messages.format(messages.getWhitelistListEmpty()));
            return;
        }

        StringBuilder out = new StringBuilder(64 + 32 * (ips.size() + players.size()));
        out.append(messages.format(messages.getWhitelistListIpsHeader(), "{count}", Integer.toString(ips.size())));
        for (String ip : ips) {
            out.append('\n').append(messages.format(messages.getWhitelistListIpEntry(), "{ip}", ip));
        }
        out.append('\n').append(messages.format(messages.getWhitelistListPlayersHeader(), "{count}", Integer.toString(players.size())));
        for (Map.Entry<UUID, String> entry : players.entrySet()) {
            String name = entry.getValue().isEmpty() ? "?" : entry.getValue();
            out.append('\n').append(messages.format(messages.getWhitelistListPlayerEntry(),
                    "{name}", name, "{uuid}", entry.getKey().toString()));
        }
        reply.accept(out.toString());
    }

    private void whitelistApply(@NotNull Consumer<String> reply, @NotNull String target, boolean add) {
        Messages messages = messages();
        if (IpUtil.isValidIp(target)) {
            boolean changed = add ? engine.getWhitelist().addIp(target) : engine.getWhitelist().removeIp(target);
            whitelistReply(reply, target, add, changed);
            return;
        }

        playerResolver.apply(target).whenComplete((uuid, error) -> {
            if (error != null) {
                reply.accept(messages.format(messages.getPlayerResolveFailed(), "{error}", describe(error)));
                return;
            }
            if (uuid == null) {
                reply.accept(messages.format(messages.getPlayerNotFound()));
                return;
            }
            boolean changed = add
                    ? engine.getWhitelist().addPlayer(uuid, target)
                    : engine.getWhitelist().removePlayer(uuid, target);
            whitelistReply(reply, target, add, changed);
        });
    }

    private void whitelistReply(@NotNull Consumer<String> reply, @NotNull String target, boolean add, boolean changed) {
        Messages messages = messages();
        String template;
        if (add) {
            template = changed ? messages.getWhitelistAdd() : messages.getWhitelistAlready();
        } else {
            template = changed ? messages.getWhitelistRemove() : messages.getWhitelistNotFound();
        }
        reply.accept(messages.format(template, "{target}", target));
    }

    private @NotNull Messages messages() {
        return engine.getMessages();
    }

    private static @NotNull String statusLabel(@NotNull ApiStatus status) {
        return status.overallStatus().toUpperCase(Locale.ROOT);
    }

    private static @NotNull String describe(@NotNull Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        String message = cause.getMessage();
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
    }
}
