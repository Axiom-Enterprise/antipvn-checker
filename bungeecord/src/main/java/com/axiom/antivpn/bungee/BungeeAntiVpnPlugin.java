package com.axiom.antivpn.bungee;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.command.OnlinePlayerNames;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import revxrsal.commands.Lamp;
import revxrsal.commands.bungee.BungeeLamp;
import revxrsal.commands.bungee.actor.BungeeCommandActor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class BungeeAntiVpnPlugin extends Plugin {

    private AntiVpnEngine engine;
    private BungeePlatform platform;

    @Override
    public void onEnable() {
        platform = new BungeePlatform(this);

        saveDefaultResource("config.yml");
        saveDefaultResource("messages.yml");

        BungeeConfig mainConfig = new BungeeConfig(new File(getDataFolder(), "config.yml"), getLogger());
        BungeeConfig messagesConfig = new BungeeConfig(new File(getDataFolder(), "messages.yml"), getLogger());

        engine = new AntiVpnEngine(platform, mainConfig, messagesConfig);

        getProxy().getPluginManager().registerListener(this, new BungeeListener(engine));

        Lamp<BungeeCommandActor> lamp = BungeeLamp.builder(this)
                .suggestionProviders(suggestions -> suggestions.addProviderForAnnotationLast(
                        OnlinePlayerNames.class, ann -> context -> onlinePlayerNames()))
                .exceptionHandler(new BungeeVpnExceptionHandler(engine))
                .build();
        lamp.register(new AntiVpnCommand(engine));
        lamp.register(new VpnWhitelistCommand(engine));

        getLogger().info("AxiomAntiVPN enabled on " + platform.getPlatformName());
    }

    private static List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
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

    private void saveDefaultResource(String name) {
        if (!getDataFolder().exists()) {
            try {
                Files.createDirectories(getDataFolder().toPath());
            } catch (IOException e) {
                getLogger().severe("Failed to create data folder");
                return;
            }
        }

        File file = new File(getDataFolder(), name);
        if (!file.exists()) {
            try (InputStream in = getResourceAsStream(name)) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                }
            } catch (IOException e) {
                getLogger().warning("Could not save default " + name);
            }
        }
    }

    public AntiVpnEngine getEngine() {
        return engine;
    }
}
