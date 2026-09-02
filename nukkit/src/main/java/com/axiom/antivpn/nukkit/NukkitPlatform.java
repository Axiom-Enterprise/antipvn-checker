package com.axiom.antivpn.nukkit;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.plugin.PluginBase;
import com.axiom.antivpn.common.platform.Platform;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

final class NukkitPlatform implements Platform {
    private final PluginBase plugin; private final Server server;
    NukkitPlatform(PluginBase plugin) { this.plugin=plugin; this.server=plugin.getServer(); }
    public String getPlatformName(){return "NukkitX";} public Logger getPluginLogger(){return Logger.getLogger("AxiomAntiVPN-NukkitX");}
    public Path getDataFolder(){return plugin.getDataFolder().toPath();} public Executor getAsyncExecutor(){return Runnable::run;}
    public void runAsync(Runnable r){server.getScheduler().scheduleTask(plugin,r,true);} public void runSync(Runnable r){server.getScheduler().scheduleTask(plugin,r);}
    public void kickPlayer(UUID id,String msg){Player p=server.getPlayer(id).orElse(null); if(p!=null)p.kick(msg);}
    public void sendMessage(UUID id,String msg){Player p=server.getPlayer(id).orElse(null); if(p!=null)p.sendMessage(msg);}
    public void broadcastPermission(String perm,String msg){server.broadcastMessage(msg);}
    public void dispatchConsoleCommand(String cmd){server.dispatchCommand(server.getConsoleSender(),cmd);}
    public boolean isPlayerOnline(UUID id){return server.getPlayer(id).isPresent();}
    public String getPlayerIp(UUID id){Player p=server.getPlayer(id).orElse(null); return p==null?null:p.getAddress();}
    public boolean hasPermission(UUID id,String perm){Player p=server.getPlayer(id).orElse(null); return p!=null&&p.hasPermission(perm);}
}
