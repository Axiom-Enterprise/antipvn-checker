package com.axiom.antivpn.velocity;

import com.axiom.antivpn.common.AntiVpnEngine;
import com.axiom.antivpn.common.util.IpUtil;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ForkJoinPool;

public final class VelocityListener {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .build();

    private final @NotNull AntiVpnEngine engine;

    public VelocityListener(@NotNull AntiVpnEngine engine) {
        this.engine = engine;
    }

    @Subscribe(order = PostOrder.LATE)
    public void onPreLogin(@NotNull PreLoginEvent event, @NotNull Continuation continuation) {
        if (!engine.getSettings().isCheckOnLogin()) {
            continuation.resume();
            return;
        }

        String ip = event.getConnection().getRemoteAddress().getAddress().getHostAddress();
        if (IpUtil.isLocalIp(ip)) {
            continuation.resume();
            return;
        }

        if (engine.getWhitelist().isIpWhitelisted(ip)) {
            continuation.resume();
            return;
        }

        engine.checkIp(ip).thenAccept(response -> {
            if (response != null && engine.shouldBlock(response)) {
                String kickMsg = engine.getMessages().format(engine.getMessages().getKickMessage(), response);
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(deserialize(kickMsg)));
                engine.alertAndLog(null, event.getUsername(), ip, response);
            }
            continuation.resume();
        }).exceptionally(ex -> {
            engine.getPlatform().getPluginLogger().warning("Failed to check IP " + ip + ": " + ex.getMessage());
            if (engine.getSettings().isBlockOnApiFailure()) {
                String kickMsg = engine.getMessages().format(engine.getMessages().getKickMessage());
                event.setResult(PreLoginEvent.PreLoginComponentResult.denied(deserialize(kickMsg)));
            }
            continuation.resume();
            return null;
        });
    }

    private static @NotNull Component deserialize(@NotNull String message) {
        return SERIALIZER.deserialize(message);
    }
}
