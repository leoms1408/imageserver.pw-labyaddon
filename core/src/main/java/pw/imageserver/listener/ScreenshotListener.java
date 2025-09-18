package pw.imageserver.listener;

import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.event.Phase;
import net.labymod.api.notification.Notification;
import net.labymod.api.notification.Notification.Builder;
import net.labymod.api.notification.Notification.NotificationButton;
import net.labymod.api.notification.Notification.Type;
import pw.imageserver.ImageserverAddon;
import pw.imageserver.api.UploadRequest;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.Style;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.misc.WriteScreenshotEvent;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class ScreenshotListener {

    private final ImageserverAddon addon;

    public ScreenshotListener(ImageserverAddon addon) {
        this.addon = addon;
    }

    @Subscribe
    public void onScreenshot(WriteScreenshotEvent event) {
        if (addon.configuration().enabled().get() == false)
            return;

        if (event.getPhase() == Phase.POST) {
            event.setCancelled(true);
            Builder builder = Notification.builder()
                .title(Component.text("imageserver.pw"))
                .text(Component.translatable("imageserver.messages.upload", Style.empty()))
                .icon(Icon.url("https://imageserver.pw/img/logo.png"))
                .addButton(NotificationButton.primary(Component.text("Upload"),
                    () -> {
                        AtomicReference<String> tokenRef = new AtomicReference<>(addon.configuration().token());

                        if (tokenRef.get() == null || tokenRef.get().isBlank()) {
                            Builder confirm = Notification.builder()
                                .title(Component.text("imageserver.pw"))
                                .text(Component.translatable("imageserver.messages.noToken", Style.empty()))
                                .icon(Icon.url("https://imageserver.pw/img/logo.png"))
                                .addButton(NotificationButton.primary(Component.text("Continue"), () -> {
                                    performUpload(event, "addon");
                                }))
                                .addButton(NotificationButton.primary(Component.text("Cancel"), () -> {
                                    Builder cancelled = Notification.builder()
                                        .title(Component.text("imageserver.pw"))
                                        .text(Component.translatable("imageserver.errors.uploadCancelled", Style.empty()))
                                        .type(Type.SYSTEM);
                                    Laby.labyAPI().notificationController().push(cancelled.build());
                                }))
                                .type(Type.SYSTEM);
                            Laby.labyAPI().notificationController().push(confirm.build());
                            return;
                        }

                        performUpload(event, tokenRef.get());
                    }))
                .type(Type.SYSTEM);
            Laby.labyAPI().notificationController().push(builder.build());
        }
    }

    private void performUpload(WriteScreenshotEvent event, String token) {
        try {
            byte[] imageBytes = event.getImage();

            if (imageBytes == null || imageBytes.length == 0) {
                Builder error = Notification.builder()
                    .title(Component.text("imageserver.pw"))
                    .text(Component.translatable("imageserver.errors.file", Style.empty()))
                    .icon(Icon.url("https://imageserver.pw/img/logo.png"))
                    .type(Type.SYSTEM);
                Laby.labyAPI().notificationController().push(error.build());
                return;
            }

            File tempFile = File.createTempFile("labymod_addon_", ".png");
            java.nio.file.Files.write(tempFile.toPath(), imageBytes);

            UploadRequest request = new UploadRequest(tempFile, token);
            request.sendAsyncRequest().thenAccept((v) -> {
                try {
                    if (request.isSuccessful()) {
                        Laby.references().chatExecutor().openUrl(request.getUploadLink(), false);
                    } else {
                        Builder error = Notification.builder()
                            .title(Component.text("imageserver.pw"))
                            .text(Component.text(request.getError()))
                            .icon(Icon.url("https://imageserver.pw/img/logo.png"))
                            .type(Type.SYSTEM);
                        Laby.labyAPI().notificationController().push(error.build());
                    }
                } finally {
                    try { tempFile.delete(); } catch (Exception ignored) { }
                }
            }).exceptionally((e) -> {
                try {
                    Builder error = Notification.builder()
                        .title(Component.text("imageserver.pw"))
                        .text(Component.text(e.getMessage()))
                        .icon(Icon.url("https://imageserver.pw/img/logo.png"))
                        .type(Type.SYSTEM);
                    Laby.labyAPI().notificationController().push(error.build());
                } finally {
                    try { tempFile.delete(); } catch (Exception ignored) { }
                }
                return null;
            });
        } catch (Exception e) {
            Builder error = Notification.builder()
                .title(Component.text("imageserver.pw"))
                .text(Component.text(e.getMessage()))
                .icon(Icon.url("https://imageserver.pw/img/logo.png"))
                .type(Type.SYSTEM);
            Laby.labyAPI().notificationController().push(error.build());
        }
    }
}
