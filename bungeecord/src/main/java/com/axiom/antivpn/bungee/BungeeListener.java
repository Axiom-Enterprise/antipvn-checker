package com.axiom.antivpn.bungee;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.check.LoginVerdict;
import com.axiom.antivpn.common.network.NetworkDecision;
import com.axiom.antivpn.common.network.NetworkDecisionCodec;
import com.axiom.antivpn.common.policy.PolicyDecision;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public final class BungeeListener implements Listener {

    private static final String NETWORK_PROXY = "PROXY";

    private final @NotNull AntiVpnEngine engine;
    private final @NotNull Plugin plugin;
    private final @NotNull ConcurrentHashMap<String, PendingDecision> pending = new ConcurrentHashMap<>();
    private final @NotNull NetworkDecisionCodec codec = new NetworkDecisionCodec();

    public BungeeListener(@NotNull AntiVpnEngine engine, @NotNull Plugin plugin) {
        this.engine = engine;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(@NotNull PreLoginEvent event) {
        if (event.isCancelled()) return;
        InetSocketAddress address = event.getConnection().getAddress();
        if (address == null || address.getAddress() == null) return;

        String ip = address.getAddress().getHostAddress();
        String name = event.getConnection().getName();
        if (!engine.shouldCheckLogin(ip, null, name)) return;

        event.registerIntent(plugin);
        engine.verifyLogin(null, name, ip).whenComplete((verdict, error) -> {
            try {
                if (error == null) {
                    apply(event, name, ip, verdict);
                }
            } finally {
                event.completeIntent(plugin);
            }
        });
    }

    private void apply(@NotNull PreLoginEvent event, @NotNull String name, @NotNull String ip, @NotNull LoginVerdict verdict) {
        if (verdict.denied()) {
            event.setCancelled(true);
            event.setCancelReason(TextComponent.fromLegacy(verdict.kickMessage()));
            return;
        }
        if (verdict.checked() && engine.getSettings().getNetworkMode().equals(NETWORK_PROXY)) {
            pending.put(name.toLowerCase(Locale.ROOT), new PendingDecision(ip, verdict.response().riskScore(), verdict.decision()));
        }
    }

    @EventHandler
    public void onServerConnected(@NotNull ServerConnectedEvent event) {
        PendingDecision value = pending.remove(event.getPlayer().getName().toLowerCase(Locale.ROOT));
        if (value == null || !engine.getSettings().getNetworkMode().equals(NETWORK_PROXY)) return;
        NetworkDecision decision = new NetworkDecision(event.getPlayer().getUniqueId(), value.ip(), value.decision().action(),
                value.decision().reason(), value.riskScore(), Instant.now().getEpochSecond());
        byte[] secret = engine.getSettings().getNetworkSecret().getBytes(StandardCharsets.UTF_8);
        event.getServer().sendData(BungeeAntiVpnPlugin.DECISION_CHANNEL, codec.encode(decision, secret));
    }

    private record PendingDecision(@NotNull String ip, int riskScore, @NotNull PolicyDecision decision) {
    }
}
