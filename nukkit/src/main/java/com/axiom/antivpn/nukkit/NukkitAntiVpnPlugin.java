package com.axiom.antivpn.nukkit;

import cn.nukkit.plugin.PluginBase;

/** NukkitX entrypoint; platform hooks are isolated for NukkitX lifecycle compatibility. */
public final class NukkitAntiVpnPlugin extends PluginBase {
    @Override
    public void onEnable() {
        getLogger().info("AxiomAntiVPN NukkitX module enabled");
    }
}
