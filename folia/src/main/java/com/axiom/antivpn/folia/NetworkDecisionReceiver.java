package com.axiom.antivpn.folia;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.network.NetworkDecision;
import com.axiom.antivpn.common.network.NetworkDecisionCodec;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public final class NetworkDecisionReceiver implements PluginMessageListener {

    private final @NotNull AntiVpnEngine engine;
    private final @NotNull NetworkDecisionCodec codec = new NetworkDecisionCodec();

    public NetworkDecisionReceiver(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] payload) {
        if (!channel.equals(FoliaAntiVpnPlugin.DECISION_CHANNEL) || !engine.getSettings().getNetworkMode().equals("BACKEND")) return;
        if (player.getAddress() == null) return;
        byte[] secret = engine.getSettings().getNetworkSecret().getBytes(StandardCharsets.UTF_8);
        NetworkDecision decision = codec.decode(payload, secret, Instant.now()).orElse(null);
        if (decision == null || !decision.playerUuid().equals(player.getUniqueId())) return;
        if (!decision.ip().equals(player.getAddress().getAddress().getHostAddress())) return;
        engine.acceptNetworkDecision(player.getName(), decision);
    }
}
