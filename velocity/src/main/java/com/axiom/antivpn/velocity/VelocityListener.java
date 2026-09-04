package com.axiom.antivpn.velocity;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.check.LoginVerdict;
import com.axiom.antivpn.common.color.ColorParser;
import com.axiom.antivpn.common.network.NetworkDecision;
import com.axiom.antivpn.common.network.NetworkDecisionCodec;
import com.axiom.antivpn.common.policy.PolicyDecision;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public final class VelocityListener {

    private static final String NETWORK_PROXY = "PROXY";

    private final @NotNull AntiVpnEngine engine;
    private final @NotNull ConcurrentHashMap<String, PendingDecision> pending = new ConcurrentHashMap<>();
    private final @NotNull NetworkDecisionCodec codec = new NetworkDecisionCodec();

    public VelocityListener(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @Subscribe(order = PostOrder.LATE)
    public void onPreLogin(@NotNull PreLoginEvent event, @NotNull Continuation continuation) {
        InetAddress address = event.getConnection().getRemoteAddress().getAddress();
        if (!event.getResult().isAllowed() || address == null) {
            continuation.resume();
            return;
        }

        String ip = address.getHostAddress();
        String name = event.getUsername();
        if (!engine.shouldCheckLogin(ip, null, name)) {
            continuation.resume();
            return;
        }

        engine.verifyLogin(null, name, ip).whenComplete((verdict, error) -> {
            if (error == null) {
                apply(event, name, ip, verdict);
            }
            continuation.resume();
        });
    }

    private void apply(@NotNull PreLoginEvent event, @NotNull String name, @NotNull String ip, @NotNull LoginVerdict verdict) {
        if (verdict.denied()) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(ColorParser.toComponent(verdict.kickMessage())));
            return;
        }
        if (verdict.checked() && engine.getSettings().getNetworkMode().equals(NETWORK_PROXY)) {
            pending.put(name.toLowerCase(Locale.ROOT), new PendingDecision(ip, verdict.response().riskScore(), verdict.decision()));
        }
    }

    @Subscribe
    public void onServerPostConnect(@NotNull ServerPostConnectEvent event) {
        PendingDecision value = pending.remove(event.getPlayer().getUsername().toLowerCase(Locale.ROOT));
        if (value == null || !engine.getSettings().getNetworkMode().equals(NETWORK_PROXY)) return;
        ServerConnection connection = event.getPlayer().getCurrentServer().orElse(null);
        if (connection == null) return;
        NetworkDecision decision = new NetworkDecision(event.getPlayer().getUniqueId(), value.ip(), value.decision().action(),
                value.decision().reason(), value.riskScore(), Instant.now().getEpochSecond());
        byte[] secret = engine.getSettings().getNetworkSecret().getBytes(StandardCharsets.UTF_8);
        connection.sendPluginMessage(VelocityAntiVpnPlugin.DECISION_CHANNEL, codec.encode(decision, secret));
    }

    private record PendingDecision(@NotNull String ip, int riskScore, @NotNull PolicyDecision decision) {
    }
}
