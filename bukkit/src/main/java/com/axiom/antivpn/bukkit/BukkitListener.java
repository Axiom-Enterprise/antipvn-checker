package com.axiom.antivpn.bukkit;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.util.IpUtil;
import com.axiom.antivpn.common.policy.EnforcementAction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;

public final class BukkitListener implements Listener {

    private final @NotNull AntiVpnEngine engine;

    public BukkitListener(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        if (!engine.getSettings().isCheckOnLogin()) return;
        if (engine.getSettings().getNetworkMode().equals("BACKEND")) return;

        String ip = event.getAddress().getHostAddress();
        if (IpUtil.isLocalIp(ip)) return;

        if (engine.getWhitelist().isIpWhitelisted(ip) || engine.getWhitelist().isPlayerWhitelisted(event.getUniqueId())) {
            return;
        }

        try {
            var response = engine.checkIp(ip).join();
            if (response == null) return;

            var decision = engine.processCheck(event.getUniqueId(), event.getName(), ip, response);
            if (decision.action() == EnforcementAction.KICK) {
                String kickMsg = engine.getMessages().format(engine.getMessages().getKickMessage(), response);
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMsg);
            }
        } catch (Exception e) {
            engine.getPlatform().getPluginLogger().warning("Failed to check IP: " + e.getClass().getSimpleName());
            engine.recordFailure(event.getUniqueId(), event.getName(), ip);
            if (engine.getSettings().isBlockOnApiFailure()) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, engine.getMessages().format(engine.getMessages().getKickMessage()));
            }
        }
    }
}
