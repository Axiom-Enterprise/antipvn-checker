package com.axiom.antivpn.minestom;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.check.LoginVerdict;
import com.axiom.antivpn.common.color.ColorParser;
import com.axiom.antivpn.common.network.NetworkDecision;
import com.axiom.antivpn.common.network.NetworkDecisionCodec;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

final class MinestomListener {

    private final @NotNull AntiVpnEngine engine;
    private final @NotNull NetworkDecisionCodec codec = new NetworkDecisionCodec();

    MinestomListener(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    void onPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        String ip = hostAddress(event.getConnection().getRemoteAddress());
        if (ip == null) return;
        LoginVerdict verdict = engine.verifyLogin(event.getPlayerUuid(), event.getUsername(), ip).join();
        if (verdict.denied()) {
            event.getConnection().kick(ColorParser.toComponent(verdict.kickMessage()));
        }
    }

    void onPluginMessage(@NotNull PlayerPluginMessageEvent event) {
        if (!AxiomAntiVpnMinestom.DECISION_CHANNEL.equals(event.getIdentifier())) return;
        if (!engine.getSettings().getNetworkMode().equals("BACKEND")) return;
        String ip = hostAddress(event.getPlayer().getPlayerConnection().getRemoteAddress());
        if (ip == null) return;
        byte[] secret = engine.getSettings().getNetworkSecret().getBytes(StandardCharsets.UTF_8);
        NetworkDecision decision = codec.decode(event.getMessage(), secret, Instant.now()).orElse(null);
        if (decision == null || !decision.playerUuid().equals(event.getPlayer().getUuid()) || !decision.ip().equals(ip)) return;
        engine.acceptNetworkDecision(event.getPlayer().getUsername(), decision);
    }

    private static @Nullable String hostAddress(@NotNull SocketAddress address) {
        return address instanceof InetSocketAddress inet && inet.getAddress() != null ? inet.getAddress().getHostAddress() : null;
    }
}
