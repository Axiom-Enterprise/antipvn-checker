package com.axiom.antivpn.nukkit;

import cn.nukkit.Player;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.plugin.PluginBase;
import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.command.VpnCommands;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class NukkitAntiVpnPlugin extends PluginBase {

    private AntiVpnEngine engine;

    @Override
    public void onEnable() {
        saveResource("config.yml");
        saveResource("messages.yml");

        NukkitPlatform platform = new NukkitPlatform(this);
        engine = new AntiVpnEngine(platform,
                new NukkitConfig(new File(getDataFolder(), "config.yml")),
                new NukkitConfig(new File(getDataFolder(), "messages.yml")));

        getServer().getPluginManager().registerEvents(new NukkitListener(engine), this);

        VpnCommands commands = new VpnCommands(engine, this::resolvePlayer);
        NukkitVpnCommand executor = new NukkitVpnCommand(commands, platform);
        if (getCommand("vpn") instanceof PluginCommand<?> command) {
            command.setExecutor(executor);
            command.setCommandParameters(NukkitVpnCommand.parameters());
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

    private @NotNull CompletableFuture<@Nullable UUID> resolvePlayer(@NotNull String name) {
        Player player = getServer().getPlayerExact(name);
        return CompletableFuture.completedFuture(player == null ? null : player.getUniqueId());
    }

    public AntiVpnEngine getEngine() {
        return engine;
    }
}
