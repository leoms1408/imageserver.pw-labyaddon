/*
 * Copyright (c) 2025 leoms1408
 * Licensed under the Apache License, Version 2.0
 */

package pw.imageserver;

import net.labymod.api.Laby;
import pw.imageserver.listener.ScreenshotListener;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.models.addon.annotation.AddonMain;

@AddonMain
public class ImageserverAddon extends LabyAddon<ImageserverConfig> {

    @Override
    protected void enable() {
        registerSettingCategory();
        Laby.labyAPI().config().notifications().screenshot().set(false);
        registerListener(new ScreenshotListener(this));
    }

    @Override
    protected Class<? extends ImageserverConfig> configurationClass() {
        return ImageserverConfig.class;
    }
}
