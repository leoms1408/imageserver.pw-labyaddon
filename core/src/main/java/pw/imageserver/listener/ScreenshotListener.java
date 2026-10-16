/*
 * Copyright (c) 2025 leoms1408
 * Licensed under the Apache License, Version 2.0
 */

package pw.imageserver.listener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.misc.ScreenshotNotificationEvent;
import net.labymod.api.notification.Notification;
import net.labymod.api.notification.Notification.Builder;
import net.labymod.api.notification.Notification.NotificationButton;
import net.labymod.api.notification.Notification.Type;
import pw.imageserver.ImageserverAddon;
import pw.imageserver.api.UploadRequest;
import pw.imageserver.api.UploadResult;
import pw.imageserver.api.UploadResult.Failure;
import pw.imageserver.api.UploadResult.Reason;
import pw.imageserver.api.UploadResult.Success;

public class ScreenshotListener {

    private final ImageserverAddon addon;
    private final Set<Path> uploadsInProgress = ConcurrentHashMap.newKeySet();

    public ScreenshotListener(ImageserverAddon addon) {
        this.addon = addon;
    }

    @Subscribe
    public void onScreenshotNotification(ScreenshotNotificationEvent event) {
        if (!addon.configuration().enabled().get())
            return;

        Path screenshotPath = event.getScreenshotPath();
        event.getNotificationBuilder()
            .clearButtons()
            .addButton(NotificationButton.primary(Component.translatable("imageserver.buttons.upload"),
                () -> {
                    String token = addon.configuration().token();

                    if (token == null || token.isBlank()) {
                        Builder confirm = notification()
                            .text(Component.translatable("imageserver.messages.noToken"))
                            .addButton(NotificationButton.primary(Component.translatable("imageserver.buttons.continue"),
                                () -> performUpload(screenshotPath, null)))
                            .addButton(NotificationButton.primary(Component.translatable("imageserver.buttons.cancel"),
                                () -> push(notification().text(Component.translatable("imageserver.errors.uploadCancelled")))));
                        push(confirm);
                        return;
                    }

                    performUpload(screenshotPath, token);
                }));
    }

    /**
     * @param token the AUTH key, or null for an anonymous upload
     */
    private void performUpload(Path screenshotPath, String token) {
        if (!Files.isRegularFile(screenshotPath)) {
            pushError(new Failure(Reason.FILE, null));
            return;
        }

        if (!uploadsInProgress.add(screenshotPath)) {
            push(notification().text(Component.translatable("imageserver.messages.alreadyUploading")));
            return;
        }

        Notification progress = notification()
            .text(Component.translatable("imageserver.messages.uploading"))
            .build();
        Laby.labyAPI().notificationController().push(progress);

        UploadRequest.upload(screenshotPath, token).thenAccept(result ->
            // Notifications and opening links must happen on the render thread
            Laby.labyAPI().minecraft().executeOnRenderThread(() -> {
                uploadsInProgress.remove(screenshotPath);
                Laby.labyAPI().notificationController().pop(progress);
                handleResult(result);
            }));
    }

    private static void handleResult(UploadResult result) {
        switch (result) {
            case Success success -> Laby.references().chatExecutor().openUrl(success.link(), false);
            case Failure failure -> pushError(failure);
        }
    }

    private static Builder notification() {
        return Notification.builder()
            .title(Component.translatable("imageserver.notification.title"))
            .icon(Icon.url("https://imageserver.pw/img/logo.png"))
            .type(Type.SYSTEM);
    }

    private static void pushError(Failure failure) {
        Component text = Component.translatable(failure.reason().translationKey());
        if (failure.detail() != null && !failure.detail().isBlank()) {
            // Server responses and exception names are dynamic and cannot be translated
            text = text.append(Component.text(" (" + failure.detail() + ")"));
        }
        push(notification().title(Component.translatable("imageserver.errors.title")).text(text));
    }

    private static void push(Builder builder) {
        Laby.labyAPI().notificationController().push(builder.build());
    }
}
