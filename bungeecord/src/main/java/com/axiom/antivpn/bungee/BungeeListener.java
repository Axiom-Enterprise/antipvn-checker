package com.axiom.antivpn.bungee;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.util.IpUtil;
import com.axiom.antivpn.common.policy.EnforcementAction;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.jetbrains.annotations.NotNull;
import com.axiom.antivpn.common.network.NetworkDecision;
import com.axiom.antivpn.common.network.NetworkDecisionCodec;
import com.axiom.antivpn.common.policy.PolicyDecision;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public final class BungeeListener implements Listener {

    private final @NotNull AntiVpnEngine engine;
    private final ConcurrentHashMap<String, PendingDecision> pending = new ConcurrentHashMap<>();
    private final NetworkDecisionCodec codec = new NetworkDecisionCodec();

    public BungeeListener(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(@NotNull PreLoginEvent event) {
        if (!engine.getSettings().isCheckOnLogin()) return;
        if (event.getConnection().getAddress() == null) return;

        String ip = event.getConnection().getAddress().getAddress().getHostAddress();
        if (IpUtil.isLocalIp(ip)) return;

        if (engine.getWhitelist().isIpWhitelisted(ip)
                || engine.getWhitelist().isPlayerNameWhitelisted(event.getConnection().getName())) return;

        event.registerIntent((net.md_5.bungee.api.plugin.Plugin) ((BungeePlatform) engine.getPlatform()).getPlugin());

        engine.checkIp(ip).thenAccept(response -> {
            try {
                if (response != null) {
                    var decision = engine.processCheck(null, event.getConnection().getName(), ip, response);
                    if (engine.getSettings().getNetworkMode().equals("PROXY") && decision.action() != EnforcementAction.KICK) {
                        pending.put(event.getConnection().getName().toLowerCase(Locale.ROOT), new PendingDecision(ip, response.riskScore(), decision));
                    }
                    if (decision.action() != EnforcementAction.KICK) return;
                    String kickMsg = engine.getMessages().format(engine.getMessages().getKickMessage(), response);
                    event.setCancelled(true);
                    event.setCancelReason(TextComponent.fromLegacy(kickMsg));
                }
            } finally {
                event.completeIntent(((BungeePlatform) engine.getPlatform()).getPlugin());
            }
        }).exceptionally(ex -> {
            try {
                engine.getPlatform().getPluginLogger().warning("Failed to check IP: " + ex.getClass().getSimpleName());
                engine.recordFailure(null, event.getConnection().getName(), ip);
                if (engine.getSettings().isBlockOnApiFailure()) {
                    event.setCancelled(true);
                    event.setCancelReason(TextComponent.fromLegacy(
                            engine.getMessages().format(engine.getMessages().getKickMessage())));
                }
            } finally {
                event.completeIntent(((BungeePlatform) engine.getPlatform()).getPlugin());
            }
            return null;
        });
    }

    @EventHandler
    public void onServerConnected(@NotNull ServerConnectedEvent event) {
        PendingDecision value = pending.remove(event.getPlayer().getName().toLowerCase(Locale.ROOT));
        if (value == null || !engine.getSettings().getNetworkMode().equals("PROXY")) return;
        NetworkDecision decision = new NetworkDecision(event.getPlayer().getUniqueId(), value.ip(), value.decision().action(),
                value.decision().reason(), value.riskScore(), Instant.now().getEpochSecond());
        byte[] secret = engine.getSettings().getNetworkSecret().getBytes(StandardCharsets.UTF_8);
        event.getServer().sendData("axiomantivpn:decision", codec.encode(decision, secret));
    }

    private record PendingDecision(@NotNull String ip, int riskScore, @NotNull PolicyDecision decision) {
    }
}
