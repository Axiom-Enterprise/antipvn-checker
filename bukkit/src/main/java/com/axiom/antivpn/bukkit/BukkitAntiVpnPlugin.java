package com.axiom.antivpn.bukkit;

import com.axiom.antivpn.bukkit.integration.AntiVpnPlaceholderExpansion;
import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.command.OnlinePlayerNames;
import com.axiom.antivpn.common.command.VpnCommands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Bukkit/Paper entrypoint. The Folia module reuses it unchanged; platform detection is runtime-based. */
public class BukkitAntiVpnPlugin extends JavaPlugin {

    public static final String DECISION_CHANNEL = "axiomantivpn:decision";

    private AntiVpnEngine engine;
    private BukkitPlatform platform;

    @Override
    public void onEnable() {
        saveResource("config.yml", false);
        saveResource("messages.yml", false);

        platform = new BukkitPlatform(this);
        engine = new AntiVpnEngine(platform,
                new BukkitConfig(new File(getDataFolder(), "config.yml"), getLogger()),
                new BukkitConfig(new File(getDataFolder(), "messages.yml"), getLogger()));

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new AntiVpnPlaceholderExpansion(this, engine).register();
        }

        getServer().getPluginManager().registerEvents(new BukkitListener(engine), this);
        getServer().getMessenger().registerIncomingPluginChannel(this, DECISION_CHANNEL, new NetworkDecisionReceiver(engine));

        VpnCommands commands = new VpnCommands(engine, BukkitAntiVpnPlugin::resolvePlayer);
        Lamp<BukkitCommandActor> lamp = BukkitLamp.builder(this)
                .suggestionProviders(suggestions -> suggestions.addProviderForAnnotationLast(
                        OnlinePlayerNames.class, ann -> context -> onlinePlayerNames()))
                .exceptionHandler(new BukkitVpnExceptionHandler(commands))
                .build();
        lamp.register(new AntiVpnCommand(commands));

        getLogger().info("AxiomAntiVPN enabled on " + platform.getPlatformName());
    }

    @Override
    public void onDisable() {
        if (engine != null) {
            engine.shutdown();
        }
        getLogger().info("AxiomAntiVPN disabled");
    }

    private static @NotNull CompletableFuture<@Nullable UUID> resolvePlayer(@NotNull String name) {
        Player player = Bukkit.getPlayerExact(name);
        return CompletableFuture.completedFuture(player == null ? null : player.getUniqueId());
    }

    private static @NotNull List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    public AntiVpnEngine getEngine() {
        return engine;
    }

    public BukkitPlatform getPlatform() {
        return platform;
    }
}
