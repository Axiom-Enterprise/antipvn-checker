package com.axiom.antivpn.bukkit;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.network.NetworkDecisionCodec;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public final class NetworkDecisionReceiver implements PluginMessageListener {
    private final AntiVpnEngine engine;
    private final NetworkDecisionCodec codec = new NetworkDecisionCodec();

    public NetworkDecisionReceiver(AntiVpnEngine engine) { this.engine = engine; }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] payload) {
        if (!channel.equals("axiomantivpn:decision") || !engine.getSettings().getNetworkMode().equals("BACKEND")) return;
        byte[] secret = engine.getSettings().getNetworkSecret().getBytes(StandardCharsets.UTF_8);
        codec.decode(payload, secret, Instant.now()).filter(decision -> decision.playerUuid().equals(player.getUniqueId()))
                .filter(decision -> player.getAddress() != null && decision.ip().equals(player.getAddress().getAddress().getHostAddress()))
                .ifPresent(decision -> engine.acceptNetworkDecision(player.getName(), decision));
    }
}
