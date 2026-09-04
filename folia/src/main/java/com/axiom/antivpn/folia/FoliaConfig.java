package com.axiom.antivpn.folia;

import com.axiom.antivpn.common.config.PluginConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FoliaConfig implements PluginConfig {

    private final @NotNull File file;
    private final @NotNull Logger logger;
    private YamlConfiguration yaml;

    public FoliaConfig(@NotNull File file, @NotNull Logger logger) {
        this.file = file;
        this.logger = logger;
        reload();
    }

    @Override
    public @NotNull String getString(@NotNull String path, @NotNull String def) {
        return yaml.getString(path, def);
    }

    @Override
    public int getInt(@NotNull String path, int def) {
        return yaml.getInt(path, def);
    }

    @Override
    public long getLong(@NotNull String path, long def) {
        return yaml.getLong(path, def);
    }

    @Override
    public boolean getBoolean(@NotNull String path, boolean def) {
        return yaml.getBoolean(path, def);
    }

    @Override
    public double getDouble(@NotNull String path, double def) {
        return yaml.getDouble(path, def);
    }

    @Override
    public @NotNull List<String> getStringList(@NotNull String path) {
        return yaml.getStringList(path);
    }

    @Override
    public @NotNull Set<String> getKeys(@NotNull String path) {
        var section = yaml.getConfigurationSection(path);
        return section != null ? section.getKeys(false) : Set.of();
    }

    @Override
    public @NotNull Map<String, Object> getSection(@NotNull String path) {
        var section = yaml.getConfigurationSection(path);
        if (section == null) return Map.of();
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            map.put(key, section.get(key));
        }
        return map;
    }

    @Override
    public boolean contains(@NotNull String path) {
        return yaml.contains(path);
    }

    @Override
    public void set(@NotNull String path, Object value) {
        yaml.set(path, value);
    }

    @Override
    public void save() {
        try {
            yaml.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save config file: " + file.getName(), e);
        }
    }

    @Override
    public void reload() {
        yaml = YamlConfiguration.loadConfiguration(file);
    }
}
