package com.axiom.antivpn.velocity;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.command.OnlinePlayerNames;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import revxrsal.commands.Lamp;
import revxrsal.commands.velocity.VelocityLamp;
import revxrsal.commands.velocity.VelocityVisitors;
import revxrsal.commands.velocity.actor.VelocityCommandActor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Plugin(
        id = "axiomantivpn",
        name = "AxiomAntiVPN",
        version = "1.0.0",
        description = "Advanced VPN & Proxy Detection powered by Axiom AntiVPN API",
        authors = {"Axiom"}
)
public final class VelocityAntiVpnPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataFolder;
    private AntiVpnEngine engine;

    @Inject
    public VelocityAntiVpnPlugin(ProxyServer server, @DataDirectory Path dataFolder) {
        this.server = server;
        this.logger = Logger.getLogger("AxiomAntiVPN");
        this.dataFolder = dataFolder;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            logger.severe("Failed to create data folder");
            return;
        }

        saveDefaultResource("config.yml");
        saveDefaultResource("messages.yml");

        VelocityPlatform platform = new VelocityPlatform(this, server, logger, dataFolder);
        VelocityConfig mainConfig = new VelocityConfig(dataFolder.resolve("config.yml"), logger);
        VelocityConfig messagesConfig = new VelocityConfig(dataFolder.resolve("messages.yml"), logger);

        engine = new AntiVpnEngine(platform, mainConfig, messagesConfig);

        server.getEventManager().register(this, new VelocityListener(engine));

        Lamp<VelocityCommandActor> lamp = VelocityLamp.builder(this, server)
                .suggestionProviders(suggestions -> suggestions.addProviderForAnnotationLast(
                        OnlinePlayerNames.class, ann -> context -> onlinePlayerNames()))
                .exceptionHandler(new VelocityVpnExceptionHandler(engine))
                .build();
        lamp.register(new AntiVpnCommand(engine));
        lamp.register(new VpnWhitelistCommand(engine, server));
        lamp.accept(VelocityVisitors.brigadier(server));

        logger.info("AxiomAntiVPN enabled on Velocity");
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : server.getAllPlayers()) {
            names.add(player.getUsername());
        }
        return names;
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (engine != null) {
            engine.shutdown();
        }
        logger.info("AxiomAntiVPN disabled");
    }

    private void saveDefaultResource(String name) {
        Path target = dataFolder.resolve(name);
        if (Files.exists(target)) return;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
            if (in != null) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            logger.warning("Could not save default " + name);
        }
    }
}
