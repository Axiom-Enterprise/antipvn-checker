package com.axiom.antivpn.velocity;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.util.IpUtil;
import com.axiom.antivpn.common.policy.EnforcementAction;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.axiom.antivpn.common.network.NetworkDecision;
import com.axiom.antivpn.common.network.NetworkDecisionCodec;
import com.axiom.antivpn.common.policy.PolicyDecision;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ForkJoinPool;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public final class VelocityListener {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .build();

    private final @NotNull AntiVpnEngine engine;
    private final ConcurrentHashMap<String, PendingDecision> pending = new ConcurrentHashMap<>();
    private final NetworkDecisionCodec codec = new NetworkDecisionCodec();

    public VelocityListener(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @Subscribe(order = PostOrder.LATE)
    public void onPreLogin(@NotNull PreLoginEvent event, @NotNull Continuation continuation) {
        if (!engine.getSettings().isCheckOnLogin()) {
            continuation.resume();
            return;
        }

        String ip = event.getConnection().getRemoteAddress().getAddress().getHostAddress();
        if (IpUtil.isLocalIp(ip)) {
            continuation.resume();
            return;
        }

        if (engine.getWhitelist().isIpWhitelisted(ip) || engine.getWhitelist().isPlayerNameWhitelisted(event.getUsername())) {
            continuation.resume();
            return;
        }

        engine.checkIp(ip).thenAccept(response -> {
            if (response != null) {
                var decision = engine.processCheck(null, event.getUsername(), ip, response);
                if (engine.getSettings().getNetworkMode().equals("PROXY") && decision.action() != EnforcementAction.KICK) {
                    pending.put(event.getUsername().toLowerCase(Locale.ROOT), new PendingDecision(ip, response.riskScore(), decision));
                }
                if (decision.action() != EnforcementAction.KICK) {
                    continuation.resume();
                    return;
                }
                String kickMsg = engine.getMessages().format(engine.getMessages().getKickMessage(), response);
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(deserialize(kickMsg)));
            }
            continuation.resume();
        }).exceptionally(ex -> {
            engine.getPlatform().getPluginLogger().warning("Failed to check IP: " + ex.getClass().getSimpleName());
            engine.recordFailure(null, event.getUsername(), ip);
            if (engine.getSettings().isBlockOnApiFailure()) {
                String kickMsg = engine.getMessages().format(engine.getMessages().getKickMessage());
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(deserialize(kickMsg)));
            }
            continuation.resume();
            return null;
        });
    }

    @Subscribe
    public void onServerPostConnect(@NotNull ServerPostConnectEvent event) {
        PendingDecision value = pending.remove(event.getPlayer().getUsername().toLowerCase(Locale.ROOT));
        if (value == null || !engine.getSettings().getNetworkMode().equals("PROXY")) return;
        NetworkDecision decision = new NetworkDecision(event.getPlayer().getUniqueId(), value.ip(), value.decision().action(),
                value.decision().reason(), value.riskScore(), Instant.now().getEpochSecond());
        byte[] secret = engine.getSettings().getNetworkSecret().getBytes(StandardCharsets.UTF_8);
        event.getPlayer().getCurrentServer().ifPresent(connection ->
                connection.sendPluginMessage(VelocityAntiVpnPlugin.DECISION_CHANNEL, codec.encode(decision, secret)));
    }

    private static @NotNull Component deserialize(@NotNull String message) {
        return SERIALIZER.deserialize(message);
    }

    private record PendingDecision(@NotNull String ip, int riskScore, @NotNull PolicyDecision decision) {
    }
}
