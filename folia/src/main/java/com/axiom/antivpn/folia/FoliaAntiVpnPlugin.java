package com.axiom.antivpn.folia;

import com.axiom.antivpn.bukkit.BukkitConfig;
import com.axiom.antivpn.bukkit.BukkitListener;
import com.axiom.antivpn.bukkit.BukkitPlatform;
import com.axiom.antivpn.bukkit.NetworkDecisionReceiver;
import com.axiom.antivpn.bukkit.AntiVpnCommand;
import com.axiom.antivpn.bukkit.BukkitVpnExceptionHandler;
import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.command.OnlinePlayerNames;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class FoliaAntiVpnPlugin extends JavaPlugin {
    private AntiVpnEngine engine;

    @Override
    public void onEnable() {
        saveResource("config.yml", false);
        saveResource("messages.yml", false);
        BukkitPlatform platform = new BukkitPlatform(this);
        engine = new AntiVpnEngine(platform, new BukkitConfig(new File(getDataFolder(), "config.yml"), getLogger()), new BukkitConfig(new File(getDataFolder(), "messages.yml"), getLogger()));
        getServer().getPluginManager().registerEvents(new BukkitListener(engine), this);
        getServer().getMessenger().registerIncomingPluginChannel(this, "axiomantivpn:decision", new NetworkDecisionReceiver(engine));
        Lamp<BukkitCommandActor> lamp = BukkitLamp.builder(this).suggestionProviders(suggestions -> suggestions.addProviderForAnnotationLast(OnlinePlayerNames.class, ann -> context -> onlinePlayerNames())).exceptionHandler(new BukkitVpnExceptionHandler(engine)).build();
        lamp.register(new AntiVpnCommand(engine));
        getLogger().info("AxiomAntiVPN enabled on Folia");
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
        return names;
    }

    @Override
    public void onDisable() { if (engine != null) engine.shutdown(); }
}
