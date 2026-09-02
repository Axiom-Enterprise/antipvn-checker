package com.axiom.antivpn.nukkit;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.event.Listener;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.player.PlayerAsyncPreLoginEvent;
import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.policy.EnforcementAction;

public final class NukkitAntiVpnPlugin extends PluginBase implements Listener {
    private AntiVpnEngine engine;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        NukkitPlatform platform = new NukkitPlatform(this);
        engine = new AntiVpnEngine(platform, new NukkitConfig(new java.io.File(getDataFolder(), "config.yml")), new NukkitConfig(new java.io.File(getDataFolder(), "messages.yml")));
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("AxiomAntiVPN NukkitX module enabled");
    }

    @EventHandler
    public void onPreLogin(PlayerAsyncPreLoginEvent event) {
        if (!engine.getSettings().isCheckOnLogin()) return;
        String ip = event.getAddress();
        try {
            var response = engine.checkIp(ip).join();
            if (response != null && engine.processCheck(event.getUuid(), event.getName(), ip, response).action() == EnforcementAction.KICK) {
                event.disAllow(engine.getMessages().format(engine.getMessages().getKickMessage(), response));
            }
        } catch (Exception ex) {
            engine.recordFailure(event.getUuid(), event.getName(), ip);
            if (engine.getSettings().isBlockOnApiFailure()) event.disAllow(engine.getMessages().format(engine.getMessages().getKickMessage()));
        }
    }

    @Override public void onDisable() { if (engine != null) engine.shutdown(); }
}
