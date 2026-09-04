package com.axiom.antivpn.folia;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.check.LoginVerdict;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;

public final class FoliaListener implements Listener {

    private final @NotNull AntiVpnEngine engine;

    public FoliaListener(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        String ip = event.getAddress().getHostAddress();
        LoginVerdict verdict = engine.verifyLogin(event.getUniqueId(), event.getName(), ip).join();
        if (verdict.denied()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    LegacyComponentSerializer.legacySection().deserialize(verdict.kickMessage()));
        }
    }
}
