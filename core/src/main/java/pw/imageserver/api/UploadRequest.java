/*
 * Copyright (c) 2025 leoms1408
 * Licensed under the Apache License, Version 2.0
 */

package pw.imageserver.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpTimeoutException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import pw.imageserver.api.UploadResult.Failure;
import pw.imageserver.api.UploadResult.Reason;
import pw.imageserver.api.UploadResult.Success;

public final class UploadRequest {

    private static final String HOST = "imageserver.pw";
    private static final URI UPLOAD_URI = URI.create("https://" + HOST + "/api/v1/upload");
    private static final URI ANONYMOUS_UPLOAD_URI = URI.create("https://" + HOST + "/upload/addon");
    // The server expects this key for anonymous uploads
    private static final String ANONYMOUS_KEY = "addon";

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_DETAIL_LENGTH = 100;

    private static final Gson GSON = new Gson();

    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private UploadRequest() {
    }

    /**
     * Uploads the given PNG file. The returned future never completes exceptionally.
     *
     * @param token the AUTH key, or null for an anonymous upload
     */
    public static CompletableFuture<UploadResult> upload(Path file, String token) {
        boolean anonymous = token == null || token.isBlank();

        return CompletableFuture
            .supplyAsync(() -> {
                try {
                    return MultipartData.newBuilder().addFile("file", file, "image/png").build();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            })
            .thenCompose(data -> CLIENT.sendAsync(
                HttpRequest.newBuilder()
                    .uri(anonymous ? ANONYMOUS_UPLOAD_URI : UPLOAD_URI)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", data.getContentType())
                    .header("X-IMAGESERVER-AUTH-KEY", anonymous ? ANONYMOUS_KEY : token)
                    .POST(data.getBodyPublisher())
                    .build(),
                BodyHandlers.ofString()))
            .thenApply(UploadRequest::toResult)
            .exceptionally(UploadRequest::toFailure);
    }

    private static UploadResult toResult(HttpResponse<String> response) {
        int status = response.statusCode();
        String body = response.body() == null ? "" : response.body().trim();

        if (status >= 200 && status <= 299) {
            // Authenticated uploads answer with {"url": "...", "deletionUrl": "..."},
            // anonymous addon uploads with the plain link
            String link = body.startsWith("{") ? jsonString(body, "url") : body;
            return link != null && isOpenableLink(link)
                ? new Success(link)
                : new Failure(Reason.INVALID_RESPONSE, null);
        }

        if (status == 401) {
            return new Failure(Reason.INVALID_KEY, null);
        }
        if (status >= 500) {
            return new Failure(Reason.SERVER, null);
        }

        // The server's error message (e.g. quota or plan limits) is more specific than ours
        String message = errorMessage(body);
        if (message != null) {
            return new Failure(Reason.REJECTED, shorten(message));
        }

        return switch (status) {
            case 413 -> new Failure(Reason.TOO_LARGE, null);
            case 429 -> new Failure(Reason.RATE_LIMITED, null);
            default -> new Failure(Reason.UNKNOWN, "HTTP " + status);
        };
    }

    private static UploadResult toFailure(Throwable throwable) {
        Throwable cause = throwable;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
            && cause.getCause() != null) {
            cause = cause.getCause();
        }

        if (cause instanceof UncheckedIOException) {
            return new Failure(Reason.FILE, null);
        }
        if (cause instanceof HttpTimeoutException) {
            return new Failure(Reason.TIMEOUT, null);
        }
        if (cause instanceof IOException) {
            return new Failure(Reason.NETWORK, null);
        }
        return new Failure(Reason.UNKNOWN, cause.getClass().getSimpleName());
    }

    /**
     * Only https links are opened in the browser. The host is not checked because
     * users can choose custom or short domains for their uploads.
     */
    private static boolean isOpenableLink(String link) {
        try {
            URI uri = new URI(link);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Errors are JSON ({"error": "..."}) except for some plain text responses.
     */
    private static String errorMessage(String body) {
        if (body.startsWith("{")) {
            return jsonString(body, "error");
        }
        // Error pages from proxies are HTML and not useful in a notification
        if (body.isEmpty() || body.startsWith("<")) {
            return null;
        }
        return body.lines().findFirst().orElse(null);
    }

    private static String jsonString(String json, String key) {
        try {
            JsonObject object = GSON.fromJson(json, JsonObject.class);
            JsonElement element = object == null ? null : object.get(key);
            return element != null && element.isJsonPrimitive() ? element.getAsString().trim() : null;
        } catch (JsonParseException | ClassCastException e) {
            return null;
        }
    }

    private static String shorten(String text) {
        return text.length() > MAX_DETAIL_LENGTH
            ? text.substring(0, MAX_DETAIL_LENGTH) + "…"
            : text;
    }
}
