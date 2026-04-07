package com.axiom.antivpn.bukkit;

import com.axiom.antivpn.common.AntiVpnEngine;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class BukkitAntiVpnPlugin extends JavaPlugin {

    private AntiVpnEngine engine;
    private BukkitPlatform platform;

    @Override
    public void onEnable() {
        platform = new BukkitPlatform(this);

        saveResource("config.yml", false);
        saveResource("messages.yml", false);

        BukkitConfig mainConfig = new BukkitConfig(new File(getDataFolder(), "config.yml"), getLogger());
        BukkitConfig messagesConfig = new BukkitConfig(new File(getDataFolder(), "messages.yml"), getLogger());

        engine = new AntiVpnEngine(platform, mainConfig, messagesConfig);

        getServer().getPluginManager().registerEvents(new BukkitListener(engine), this);

        var command = getCommand("antivpn");
        if (command != null) {
            BukkitCommand cmd = new BukkitCommand(engine);
            command.setExecutor(cmd);
            command.setTabCompleter(cmd);
        }

        getLogger().info("AxiomAntiVPN enabled on " + platform.getPlatformName());
    }

    @Override
    public void onDisable() {
        if (engine != null) {
            engine.shutdown();
        }
        getLogger().info("AxiomAntiVPN disabled");
    }

    public AntiVpnEngine getEngine() {
        return engine;
    }
}
