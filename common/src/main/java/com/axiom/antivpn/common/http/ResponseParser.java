package com.axiom.antivpn.common.http;

import com.axiom.antivpn.api.model.ApiStatus;
import com.axiom.antivpn.api.model.VpnResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResponseParser {

    @NotNull
    public static VpnResponse parseCheckResponse(@NotNull String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.get("success").getAsBoolean()) {
            String error = root.has("error") ? root.get("error").getAsString() : "Unknown error";
            throw new RuntimeException("API error: " + error);
        }
        JsonObject data = root.getAsJsonObject("data");
        return mapVpnResponse(data);
    }

    public static @NotNull List<VpnResponse> parseBatchResponse(@NotNull String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.get("success").getAsBoolean()) {
            String error = root.has("error") ? root.get("error").getAsString() : "Unknown error";
            throw new RuntimeException("API error: " + error);
        }
        JsonArray results = root.getAsJsonArray("data");
        List<VpnResponse> responses = new ArrayList<>(results.size());
        for (JsonElement element : results) {
            responses.add(mapVpnResponse(element.getAsJsonObject()));
        }
        return responses;
    }

    public static @NotNull ApiStatus parseStatusResponse(@NotNull String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject data = root.getAsJsonObject("data");

        String overallStatus = getString(data, "overall_status", "unknown");
        Map<String, ApiStatus.ServiceStatus> services = new LinkedHashMap<>();

        if (data.has("services")) {
            JsonObject svcs = data.getAsJsonObject("services");
            for (String key : svcs.keySet()) {
                JsonObject svc = svcs.getAsJsonObject(key);
                services.put(key, new ApiStatus.ServiceStatus(
                        getString(svc, "status", "unknown"),
                        svc.has("response_time_ms") ? svc.get("response_time_ms").getAsDouble() : 0.0,
                        getString(svc, "message", "")
                ));
            }
        }
        return new ApiStatus(overallStatus, services);
    }

    private static @NotNull VpnResponse mapVpnResponse(@NotNull JsonObject data) {
        JsonObject location = data.has("location") && !data.get("location").isJsonNull()
                ? data.getAsJsonObject("location") : null;

        return new VpnResponse(
                getString(data, "ip", "0.0.0.0"),
                getBool(data, "is_vpn"),
                getBool(data, "is_proxy"),
                getBool(data, "is_tor"),
                getBool(data, "is_datacenter"),
                getBool(data, "is_residential"),
                false,
                getInt(data, "threat_score", getInt(data, "score", 0)),
                getStringOrNull(data, "org"),
                getStringOrNull(data, "country_name"),
                getStringOrNull(data, "country"),
                getStringOrNull(data, "city"),
                getStringOrNull(data, "region"),
                location != null ? getDouble(location, "latitude") : getDouble(data, "latitude"),
                location != null ? getDouble(location, "longitude") : getDouble(data, "longitude"),
                location != null ? getStringOrNull(location, "timezone") : getStringOrNull(data, "timezone"),
                getStringOrNull(data, "asn"),
                getStringOrNull(data, "org"),
                false,
                System.currentTimeMillis()
        );
    }

    private static @NotNull String getString(@NotNull JsonObject obj, @NotNull String key, @NotNull String def) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : def;
    }

    private static String getStringOrNull(@NotNull JsonObject obj, @NotNull String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private static boolean getBool(@NotNull JsonObject obj, @NotNull String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() && obj.get(key).getAsBoolean();
    }

    private static int getInt(@NotNull JsonObject obj, @NotNull String key, int def) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : def;
    }

    private static double getDouble(@NotNull JsonObject obj, @NotNull String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsDouble() : 0.0;
    }
}
