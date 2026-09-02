package com.axiom.antivpn.minestom;

import com.axiom.antivpn.common.platform.Platform;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

final class MinestomPlatform implements Platform {
    private final Logger logger=Logger.getLogger("AxiomAntiVPN-Minestom");
    public String getPlatformName(){return "Minestom";} public Logger getPluginLogger(){return logger;} public Path getDataFolder(){return Path.of("antivpn");}
    public Executor getAsyncExecutor(){return Runnable::run;} public void runAsync(Runnable r){r.run();} public void runSync(Runnable r){r.run();}
    private Player player(UUID id){return MinecraftServer.getConnectionManager().getOnlinePlayers().stream().filter(p->p.getUuid().equals(id)).findFirst().orElse(null);}
    public void kickPlayer(UUID id,String msg){Player p=player(id);if(p!=null)p.kick(net.kyori.adventure.text.Component.text(msg));} public void sendMessage(UUID id,String msg){Player p=player(id);if(p!=null)p.sendMessage(msg);}
    public void broadcastPermission(String p,String m){MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(x->x.sendMessage(m));} public void dispatchConsoleCommand(String c){MinecraftServer.getCommandManager().executeServerCommand(c);}
    public boolean isPlayerOnline(UUID id){return player(id)!=null;} public String getPlayerIp(UUID id){Player p=player(id);return p==null?null:((java.net.InetSocketAddress)p.getPlayerConnection().getRemoteAddress()).getAddress().getHostAddress();}
    public boolean hasPermission(UUID id,String p){return false;}
}
