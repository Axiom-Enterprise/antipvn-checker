package com.axiom.antivpn.bukkit;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.util.IpUtil;
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

        String ip = event.getAddress().getHostAddress();
        if (IpUtil.isLocalIp(ip)) return;

        try {
            var response = engine.checkIp(ip).join();
            if (response == null) return;

            if (engine.shouldBlock(response)) {
                String kickMsg = engine.getMessages().format(engine.getMessages().getKickMessage(), response);
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, kickMsg);
                engine.alertAndLog(event.getUniqueId(), event.getName(), ip, response);
            }
        } catch (Exception e) {
            engine.getPlatform().getPluginLogger().warning("Failed to check IP " + ip + ": " + e.getMessage());
            if (engine.getSettings().isBlockOnApiFailure()) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, engine.getMessages().format(engine.getMessages().getKickMessage()));
            }
        }
    }
}
