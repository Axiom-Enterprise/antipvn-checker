package com.axiom.antivpn.minestom;

import com.axiom.antivpn.common.config.PluginConfig;
import java.util.*;

final class MinestomConfig implements PluginConfig {
    private final Properties values = new Properties();
    MinestomConfig() { values.setProperty("api.key", ""); values.setProperty("api.base-url", "https://antivpn.mathsanalysis.com/api"); values.setProperty("detection.risk-score-threshold", "75"); values.setProperty("connection.check-on-login", "true"); }
    public String getString(String p,String d){return values.getProperty(p,d);} public int getInt(String p,int d){return Integer.parseInt(values.getProperty(p,String.valueOf(d)));}
    public long getLong(String p,long d){return Long.parseLong(values.getProperty(p,String.valueOf(d)));} public boolean getBoolean(String p,boolean d){return Boolean.parseBoolean(values.getProperty(p,String.valueOf(d)));}
    public double getDouble(String p,double d){return Double.parseDouble(values.getProperty(p,String.valueOf(d)));} public List<String> getStringList(String p){return List.of("VPN","PROXY","TOR","DATACENTER");}
    public Set<String> getKeys(String p){return Set.of();} public Map<String,Object> getSection(String p){return Map.of();} public boolean contains(String p){return values.containsKey(p);}
    public void set(String p,Object v){values.setProperty(p,String.valueOf(v));} public void save(){} public void reload(){}
}
