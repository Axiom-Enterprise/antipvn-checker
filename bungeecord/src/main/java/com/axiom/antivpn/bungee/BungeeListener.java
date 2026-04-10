package com.axiom.antivpn.bungee;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.util.IpUtil;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.jetbrains.annotations.NotNull;

public final class BungeeListener implements Listener {

    private final @NotNull AntiVpnEngine engine;

    public BungeeListener(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(@NotNull PreLoginEvent event) {
        if (!engine.getSettings().isCheckOnLogin()) return;
        if (event.getConnection().getAddress() == null) return;

        String ip = event.getConnection().getAddress().getAddress().getHostAddress();
        if (IpUtil.isLocalIp(ip)) return;

        event.registerIntent((net.md_5.bungee.api.plugin.Plugin) ((BungeePlatform) engine.getPlatform()).getPlugin());

        engine.checkIp(ip).thenAccept(response -> {
            try {
                if (response != null && engine.shouldBlock(response)) {
                    String kickMsg = engine.getMessages().format(engine.getMessages().getKickMessage(), response);
                    event.setCancelled(true);
                    event.setCancelReason(TextComponent.fromLegacy(kickMsg));

                    engine.alertAndLog(null, event.getConnection().getName(), ip, response);
                }
            } finally {
                event.completeIntent(((BungeePlatform) engine.getPlatform()).getPlugin());
            }
        }).exceptionally(ex -> {
            try {
                engine.getPlatform().getPluginLogger().warning("Failed to check IP " + ip + ": " + ex.getMessage());
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
}
