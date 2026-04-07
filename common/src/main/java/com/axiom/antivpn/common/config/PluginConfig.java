package com.axiom.antivpn.common.config;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PluginConfig {

    @NotNull String getString(@NotNull String path, @NotNull String def);

    int getInt(@NotNull String path, int def);

    long getLong(@NotNull String path, long def);

    boolean getBoolean(@NotNull String path, boolean def);

    double getDouble(@NotNull String path, double def);

    @NotNull List<String> getStringList(@NotNull String path);

    @NotNull Set<String> getKeys(@NotNull String path);

    @NotNull Map<String, Object> getSection(@NotNull String path);

    boolean contains(@NotNull String path);

    void set(@NotNull String path, Object value);

    void save();

    void reload();
}
