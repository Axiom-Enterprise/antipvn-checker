package com.axiom.antivpn.nukkit;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerAsyncPreLoginEvent;
import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.check.LoginVerdict;
import org.jetbrains.annotations.NotNull;

final class NukkitListener implements Listener {

    private final @NotNull AntiVpnEngine engine;

    NukkitListener(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    /** Fired on Nukkit's async login pool, so blocking on the API result is safe here. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPreLogin(@NotNull PlayerAsyncPreLoginEvent event) {
        if (event.getLoginResult() != PlayerAsyncPreLoginEvent.LoginResult.SUCCESS) return;
        LoginVerdict verdict = engine.verifyLogin(event.getUuid(), event.getName(), event.getAddress()).join();
        if (verdict.denied()) {
            event.disAllow(BedrockText.render(verdict.kickMessage()));
        }
    }
}
