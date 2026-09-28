/*
 * Copyright (c) 2025 leoms1408
 * Licensed under the Apache License, Version 2.0
 */

package pw.imageserver.listener;

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

import java.nio.file.Files;
import java.nio.file.Path;

public class ScreenshotListener {

    private final ImageserverAddon addon;

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
                                () -> performUpload(screenshotPath, "addon")))
                            .addButton(NotificationButton.primary(Component.translatable("imageserver.buttons.cancel"),
                                () -> push(notification().text(Component.translatable("imageserver.errors.uploadCancelled")))));
                        push(confirm);
                        return;
                    }

                    performUpload(screenshotPath, token);
                }));
    }

    private void performUpload(Path screenshotPath, String token) {
        if (!Files.isRegularFile(screenshotPath)) {
            push(notification().text(Component.translatable("imageserver.errors.file")));
            return;
        }

        UploadRequest request = new UploadRequest(screenshotPath.toFile(), token);
        request.sendAsyncRequest().thenAccept((v) -> {
            if (request.isSuccessful()) {
                Laby.references().chatExecutor().openUrl(request.getUploadLink(), false);
            } else {
                pushError(request.getError());
            }
        }).exceptionally((e) -> {
            pushError(e.getMessage());
            return null;
        });
    }

    private static Builder notification() {
        return Notification.builder()
            .title(Component.translatable("imageserver.notification.title"))
            .icon(Icon.url("https://imageserver.pw/img/logo.png"))
            .type(Type.SYSTEM);
    }

    private static void pushError(String message) {
        // Server responses and exception messages are dynamic and cannot be translated
        Component text = message == null || message.isBlank()
            ? Component.translatable("imageserver.errors.unknown")
            : Component.text(message);
        push(notification().title(Component.translatable("imageserver.errors.title")).text(text));
    }

    private static void push(Builder builder) {
        Laby.labyAPI().notificationController().push(builder.build());
    }
}
