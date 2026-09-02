package com.axiom.antivpn.minestom;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.policy.EnforcementAction;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.kyori.adventure.text.Component;

/** Minestom integration entrypoint; register it from the server bootstrap. */
public final class AxiomAntiVpnMinestom {
    private AxiomAntiVpnMinestom() { }

    public static AxiomAntiVpnMinestom create() {
        AxiomAntiVpnMinestom plugin = new AxiomAntiVpnMinestom();
        plugin.install();
        return plugin;
    }

    private void install() {
        AntiVpnEngine engine = new AntiVpnEngine(new MinestomPlatform(), new MinestomConfig(), new MinestomConfig());
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerPreLoginEvent.class, event -> {
            String ip = ((java.net.InetSocketAddress) event.getConnection().getRemoteAddress()).getAddress().getHostAddress();
            try {
                var response = engine.checkIp(ip).join();
                if (response != null && engine.processCheck(event.getPlayerUuid(), event.getUsername(), ip, response).action() == EnforcementAction.KICK) {
                    event.getConnection().kick(Component.text(engine.getMessages().format(engine.getMessages().getKickMessage(), response)));
                }
            } catch (Exception ignored) {
                engine.recordFailure(event.getPlayerUuid(), event.getUsername(), ip);
            }
        });
    }
}
