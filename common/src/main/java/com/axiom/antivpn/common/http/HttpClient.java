package com.axiom.antivpn.common.http;

import com.axiom.antivpn.common.config.Settings;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HttpClient {

    private final java.net.http.HttpClient client;
    private final @NotNull Settings settings;
    private final @NotNull Logger logger;

    public HttpClient(@NotNull Settings settings, @NotNull Executor executor, @NotNull Logger logger) {
        this.settings = settings;
        this.logger = logger;
        this.client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(settings.getApiTimeoutMs()))
                .executor(executor)
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();
    }

    public @NotNull CompletableFuture<String> getAsync(@NotNull String path) {
        String url = settings.getApiBaseUrl() + path;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-API-Key", settings.getApiKey())
                .header("Accept", "application/json")
                .header("User-Agent", "AxiomAntiVPN-Plugin/1.0")
                .timeout(Duration.ofMillis(settings.getApiTimeoutMs()))
                .GET()
                .build();

        return executeWithRetry(request, 0);
    }

    public @NotNull CompletableFuture<String> postAsync(@NotNull String path, @NotNull String jsonBody) {
        String url = settings.getApiBaseUrl() + path;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-API-Key", settings.getApiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "AxiomAntiVPN-Plugin/1.0")
                .timeout(Duration.ofMillis(settings.getApiTimeoutMs()))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return executeWithRetry(request, 0);
    }

    private @NotNull CompletableFuture<String> executeWithRetry(@NotNull HttpRequest request, int attempt) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenCompose(response -> {
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return CompletableFuture.completedFuture(response.body());
            }
            if (status == 429 || status >= 500) {
                if (attempt < settings.getApiMaxRetries()) {
                    long delay = (long) Math.pow(2, attempt) * 500L;
                    return delayThen(delay)
                            .thenCompose(v -> executeWithRetry(request, attempt + 1));
                }
            }
            return CompletableFuture.failedFuture(
                    new ApiException(status, response.body()));
        }).exceptionallyCompose(ex -> {
            if (ex.getCause() instanceof ApiException) {
                return CompletableFuture.failedFuture(ex.getCause());
            }
            if (attempt < settings.getApiMaxRetries()) {
                logger.log(Level.WARNING, "API request failed (attempt " + (attempt + 1) + "), retrying: " + ex.getMessage());
                long delay = (long) Math.pow(2, attempt) * 500L;
                return delayThen(delay).thenCompose(v -> executeWithRetry(request, attempt + 1));
            }
            return CompletableFuture.failedFuture(ex);
        });
    }

    private @NotNull CompletableFuture<Void> delayThen(long millis) {
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void shutdown() {
    }
}
