package com.axiom.antivpn.velocity;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

        var meta = server.getCommandManager().metaBuilder("antivpn")
                .aliases("avpn", "axiomvpn")
                .plugin(this)
                .build();

        server.getCommandManager().register(meta, new VelocityCommand(engine));

        logger.info("AxiomAntiVPN enabled on Velocity");
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
