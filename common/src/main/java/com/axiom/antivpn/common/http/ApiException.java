package com.axiom.antivpn.common.http;

import org.jetbrains.annotations.NotNull;

public final class ApiException extends Exception {

    private final int statusCode;
    private final @NotNull String responseBody;

    public ApiException(int statusCode, @NotNull String responseBody) {
        super("API returned status " + statusCode + ": " + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public @NotNull String getResponseBody() {
        return responseBody;
    }
}
