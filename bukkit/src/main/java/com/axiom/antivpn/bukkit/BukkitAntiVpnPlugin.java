package com.axiom.antivpn.bukkit;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.command.OnlinePlayerNames;
import com.axiom.antivpn.bukkit.integration.AntiVpnPlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new AntiVpnPlaceholderExpansion(engine).register();
        }

        getServer().getPluginManager().registerEvents(new BukkitListener(engine), this);
        getServer().getMessenger().registerIncomingPluginChannel(this, "axiomantivpn:decision", new NetworkDecisionReceiver(engine));

        Lamp<BukkitCommandActor> lamp = BukkitLamp.builder(this)
                .suggestionProviders(suggestions -> suggestions.addProviderForAnnotationLast(
                        OnlinePlayerNames.class, ann -> context -> onlinePlayerNames()))
                .exceptionHandler(new BukkitVpnExceptionHandler(engine))
                .build();
        lamp.register(new AntiVpnCommand(engine));

        getLogger().info("AxiomAntiVPN enabled on " + platform.getPlatformName());
    }

    private static List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
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
