package com.axiom.antivpn.velocity;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.command.OnlinePlayerNames;
import com.axiom.antivpn.common.command.VpnCommands;
import com.axiom.antivpn.common.config.YamlConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.Lamp;
import revxrsal.commands.velocity.VelocityLamp;
import revxrsal.commands.velocity.VelocityVisitors;
import revxrsal.commands.velocity.actor.VelocityCommandActor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@Plugin(
        id = "axiomantivpn",
        name = "AxiomAntiVPN",
        version = BuildVersion.VALUE,
        description = "Advanced VPN & Proxy Detection powered by Axiom AntiVPN API",
        authors = {"Axiom"}
)
public final class VelocityAntiVpnPlugin {

    public static final MinecraftChannelIdentifier DECISION_CHANNEL = MinecraftChannelIdentifier.from("axiomantivpn:decision");
    private static final String MOJANG_PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";

    private final @NotNull ProxyServer server;
    private final @NotNull Logger logger;
    private final @NotNull Path dataFolder;
    private final @NotNull HttpClient mojangClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private AntiVpnEngine engine;

    @Inject
    public VelocityAntiVpnPlugin(@NotNull ProxyServer server, @DataDirectory @NotNull Path dataFolder) {
        this.server = server;
        this.logger = Logger.getLogger("AxiomAntiVPN");
        this.dataFolder = dataFolder;
    }

    @Subscribe
    public void onProxyInitialize(@NotNull ProxyInitializeEvent event) {
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            logger.severe("Failed to create data folder");
            return;
        }
        server.getChannelRegistrar().register(DECISION_CHANNEL);

        saveDefaultResource("config.yml");
        saveDefaultResource("messages.yml");

        VelocityPlatform platform = new VelocityPlatform(this, server, logger, dataFolder);
        engine = new AntiVpnEngine(platform,
                new YamlConfig(dataFolder.resolve("config.yml"), logger),
                new YamlConfig(dataFolder.resolve("messages.yml"), logger));

        server.getEventManager().register(this, new VelocityListener(engine));

        VpnCommands commands = new VpnCommands(engine, this::resolvePlayer);
        Lamp<VelocityCommandActor> lamp = VelocityLamp.builder(this, server)
                .suggestionProviders(suggestions -> suggestions.addProviderForAnnotationLast(
                        OnlinePlayerNames.class, ann -> context -> onlinePlayerNames()))
                .exceptionHandler(new VelocityVpnExceptionHandler(commands))
                .build();
        lamp.register(new AntiVpnCommand(commands));
        lamp.accept(VelocityVisitors.brigadier(server));

        logger.info("AxiomAntiVPN enabled on Velocity");
    }

    @Subscribe
    public void onProxyShutdown(@NotNull ProxyShutdownEvent event) {
        server.getChannelRegistrar().unregister(DECISION_CHANNEL);
        if (engine != null) {
            engine.shutdown();
        }
        logger.info("AxiomAntiVPN disabled");
    }

    private @NotNull CompletableFuture<@Nullable UUID> resolvePlayer(@NotNull String name) {
        Player online = server.getPlayer(name).orElse(null);
        if (online != null) {
            return CompletableFuture.completedFuture(online.getUniqueId());
        }
        if (!server.getConfiguration().isOnlineMode()) {
            return CompletableFuture.completedFuture(
                    UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOJANG_PROFILE_URL + URLEncoder.encode(name, StandardCharsets.UTF_8)))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        return mojangClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() != 200 || response.body().isBlank()) {
                return null;
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            return undashedUuid(json.get("id").getAsString());
        });
    }

    private static @NotNull UUID undashedUuid(@NotNull String id) {
        return UUID.fromString(id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
    }

    private @NotNull List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : server.getAllPlayers()) {
            names.add(player.getUsername());
        }
        return names;
    }

    private void saveDefaultResource(@NotNull String name) {
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
