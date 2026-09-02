package com.axiom.antivpn.nukkit;

import cn.nukkit.utils.Config;
import com.axiom.antivpn.common.config.PluginConfig;
import java.util.*;

final class NukkitConfig implements PluginConfig {
    private final Config config;
    NukkitConfig(java.io.File file) { config = new Config(file, Config.YAML); }
    public String getString(String p, String d) { return config.getString(p, d); }
    public int getInt(String p, int d) { return config.getInt(p, d); }
    public long getLong(String p, long d) { return config.getLong(p, d); }
    public boolean getBoolean(String p, boolean d) { return config.getBoolean(p, d); }
    public double getDouble(String p, double d) { return config.getDouble(p, d); }
    public List<String> getStringList(String p) { return config.getStringList(p); }
    public Set<String> getKeys(String p) { Object v=config.get(p); if (!(v instanceof Map<?,?> m)) return Set.of(); Set<String> keys=new LinkedHashSet<>(); for(Object key:m.keySet()) keys.add(String.valueOf(key)); return keys; }
    public Map<String,Object> getSection(String p) { Object v=config.get(p); if (!(v instanceof Map<?,?> m)) return Map.of(); Map<String,Object> out=new LinkedHashMap<>(); m.forEach((k,val)->out.put(String.valueOf(k),val)); return out; }
    public boolean contains(String p) { return config.exists(p); }
    public void set(String p,Object v) { config.set(p,v); }
    public void save() { config.save(); }
    public void reload() { config.reload(); }
}
