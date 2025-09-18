package pw.imageserver.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.labymod.api.util.I18n;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class UploadRequest {

    private boolean successful;
    private String uploadLink;
    protected String error;

    private final File file;
    private final String token;

    public UploadRequest(File file, String token) {
        this.file = file;
        this.token = token;
    }

    public CompletableFuture<Void> sendAsyncRequest() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            MultipartData data = MultipartData.newBuilder().addFile("file", file.toPath(), "image/png").build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(
                    Objects.equals(this.token, "addon") ? "https://imageserver.pw/upload/addon" : "https://imageserver.pw/upload"))
                .header("Content-Type", data.getContentType())
                .header("X-IMAGESERVER-AUTH-KEY", token)
                .method("POST", data.getBodyPublisher())
                .build();

            HttpClient client = HttpClient.newHttpClient();
            client
                .sendAsync(request, BodyHandlers.ofString())
                .thenAccept((response) -> {
                    successful = response.statusCode() >= 200 && response.statusCode() <= 299;
                    if(successful) {
                        uploadLink = response.body();
                    } else {
                        error = response.body();
                    }
                    future.complete(null);
                })
                .exceptionally((e) -> {
                    future.completeExceptionally(e);
                    error = e.getMessage();
                    return null;
                });
        } catch (Exception e) {
            error = e.getMessage();
            future.completeExceptionally(e);
        }

        return future;
    }

    public boolean isSuccessful() {
        return successful;
    }
    public String getUploadLink() {
        return uploadLink;
    }
    public String getError() {
        return error;
    }
}