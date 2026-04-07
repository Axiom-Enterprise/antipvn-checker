package com.axiom.antivpn.bungee;

import com.axiom.antivpn.common.config.PluginConfig;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BungeeConfig implements PluginConfig {

    private final @NotNull File file;
    private final @NotNull Logger logger;
    private Configuration config;

    public BungeeConfig(@NotNull File file, @NotNull Logger logger) {
        this.file = file;
        this.logger = logger;
        reload();
    }

    @Override
    public @NotNull String getString(@NotNull String path, @NotNull String def) {
        return config.getString(path, def);
    }

    @Override
    public int getInt(@NotNull String path, int def) {
        return config.getInt(path, def);
    }

    @Override
    public long getLong(@NotNull String path, long def) {
        return config.getLong(path, def);
    }

    @Override
    public boolean getBoolean(@NotNull String path, boolean def) {
        return config.getBoolean(path, def);
    }

    @Override
    public double getDouble(@NotNull String path, double def) {
        return config.getDouble(path, def);
    }

    @Override
    public @NotNull List<String> getStringList(@NotNull String path) {
        return config.getStringList(path);
    }

    @Override
    public @NotNull Set<String> getKeys(@NotNull String path) {
        Configuration section = config.getSection(path);
        return section != null ? new LinkedHashSet<>(section.getKeys()) : Set.of();
    }

    @Override
    public @NotNull Map<String, Object> getSection(@NotNull String path) {
        Configuration section = config.getSection(path);
        if (section == null) return Map.of();
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : section.getKeys()) {
            map.put(key, section.get(key));
        }
        return map;
    }

    @Override
    public boolean contains(@NotNull String path) {
        return config.contains(path);
    }

    @Override
    public void set(@NotNull String path, Object value) {
        config.set(path, value);
    }

    @Override
    public void save() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save config: " + file.getName(), e);
        }
    }

    @Override
    public void reload() {
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load config: " + file.getName(), e);
            config = new Configuration();
        }
    }
}
