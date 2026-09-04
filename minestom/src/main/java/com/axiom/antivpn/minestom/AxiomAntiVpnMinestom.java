package com.axiom.antivpn.minestom;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.command.VpnCommands;
import com.axiom.antivpn.common.config.YamlConfig;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AxiomAntiVpnMinestom {

    public static final String DECISION_CHANNEL = "axiomantivpn:decision";

    private final @NotNull AntiVpnEngine engine;
    private final @NotNull MinestomPlatform platform;

    private AxiomAntiVpnMinestom(@NotNull Path dataDirectory, @NotNull PermissionChecker permissions) {
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create AntiVPN data directory " + dataDirectory, e);
        }
        saveDefaultResource(dataDirectory, "config.yml");
        saveDefaultResource(dataDirectory, "messages.yml");

        this.platform = new MinestomPlatform(dataDirectory, permissions);
        this.engine = new AntiVpnEngine(platform,
                new YamlConfig(dataDirectory.resolve("config.yml"), platform.getPluginLogger()),
                new YamlConfig(dataDirectory.resolve("messages.yml"), platform.getPluginLogger()));

        MinestomListener listener = new MinestomListener(engine);
        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();
        events.addListener(AsyncPlayerPreLoginEvent.class, listener::onPreLogin);
        events.addListener(PlayerPluginMessageEvent.class, listener::onPluginMessage);

        VpnCommands commands = new VpnCommands(engine, AxiomAntiVpnMinestom::resolvePlayer);
        MinecraftServer.getCommandManager().register(new MinestomVpnCommand(commands, permissions));
        MinecraftServer.getSchedulerManager().buildShutdownTask(this::shutdown);

        platform.getPluginLogger().info("AxiomAntiVPN enabled on " + platform.getPlatformName());
    }

    public static @NotNull AxiomAntiVpnMinestom create() {
        return create(Path.of("antivpn"), PermissionChecker.CONSOLE_ONLY);
    }

    public static @NotNull AxiomAntiVpnMinestom create(@NotNull Path dataDirectory, @NotNull PermissionChecker permissions) {
        return new AxiomAntiVpnMinestom(dataDirectory, permissions);
    }

    public void shutdown() {
        engine.shutdown();
        platform.close();
    }

    public @NotNull AntiVpnEngine engine() {
        return engine;
    }

    private static @NotNull CompletableFuture<@Nullable UUID> resolvePlayer(@NotNull String name) {
        Player player = MinecraftServer.getConnectionManager().getOnlinePlayerByUsername(name);
        return CompletableFuture.completedFuture(player == null ? null : player.getUuid());
    }

    private static void saveDefaultResource(@NotNull Path directory, @NotNull String name) {
        Path target = directory.resolve(name);
        if (Files.exists(target)) return;
        try (InputStream in = AxiomAntiVpnMinestom.class.getClassLoader().getResourceAsStream(name)) {
            if (in != null) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot save default " + name, e);
        }
    }
}
