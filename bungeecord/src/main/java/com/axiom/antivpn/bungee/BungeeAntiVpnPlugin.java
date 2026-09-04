package com.axiom.antivpn.bungee;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.command.OnlinePlayerNames;
import com.axiom.antivpn.common.command.VpnCommands;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.Lamp;
import revxrsal.commands.bungee.BungeeLamp;
import revxrsal.commands.bungee.actor.BungeeCommandActor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class BungeeAntiVpnPlugin extends Plugin {

    public static final String DECISION_CHANNEL = "axiomantivpn:decision";

    private AntiVpnEngine engine;
    private BungeePlatform platform;

    @Override
    public void onEnable() {
        platform = new BungeePlatform(this);
        getProxy().registerChannel(DECISION_CHANNEL);

        saveDefaultResource("config.yml");
        saveDefaultResource("messages.yml");

        engine = new AntiVpnEngine(platform,
                new BungeeConfig(new File(getDataFolder(), "config.yml"), getLogger()),
                new BungeeConfig(new File(getDataFolder(), "messages.yml"), getLogger()));

        getProxy().getPluginManager().registerListener(this, new BungeeListener(engine, this));

        VpnCommands commands = new VpnCommands(engine, BungeeAntiVpnPlugin::resolvePlayer);
        Lamp<BungeeCommandActor> lamp = BungeeLamp.builder(this)
                .suggestionProviders(suggestions -> suggestions.addProviderForAnnotationLast(
                        OnlinePlayerNames.class, ann -> context -> onlinePlayerNames()))
                .exceptionHandler(new BungeeVpnExceptionHandler(commands))
                .build();
        lamp.register(new AntiVpnCommand(commands));

        getLogger().info("AxiomAntiVPN enabled on " + platform.getPlatformName());
    }

    @Override
    public void onDisable() {
        getProxy().unregisterChannel(DECISION_CHANNEL);
        if (engine != null) {
            engine.shutdown();
        }
        getLogger().info("AxiomAntiVPN disabled");
    }

    private static @NotNull CompletableFuture<@Nullable UUID> resolvePlayer(@NotNull String name) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(name);
        return CompletableFuture.completedFuture(player == null ? null : player.getUniqueId());
    }

    private static @NotNull List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private void saveDefaultResource(@NotNull String name) {
        File file = new File(getDataFolder(), name);
        if (file.exists()) return;
        try (InputStream in = getResourceAsStream(name)) {
            if (in == null) return;
            Files.createDirectories(getDataFolder().toPath());
            Files.copy(in, file.toPath());
        } catch (IOException e) {
            getLogger().warning("Could not save default " + name);
        }
    }

    public AntiVpnEngine getEngine() {
        return engine;
    }
}
