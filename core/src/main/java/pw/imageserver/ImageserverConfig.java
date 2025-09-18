/*
 * Copyright (c) 2025 leoms1408
 * Licensed under the Apache License, Version 2.0
 */

package pw.imageserver;

import net.labymod.api.Laby;
import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.configuration.loader.annotation.SpriteSlot;
import net.labymod.api.configuration.loader.annotation.SpriteTexture;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.configuration.settings.annotation.SettingSection;
import net.labymod.api.util.MethodOrder;
import java.util.Timer;
import java.util.TimerTask;

@SpriteTexture("settings")
public class ImageserverConfig extends AddonConfig {

    @SettingSection("register")
    @SpriteSlot(size = 32)
    @SwitchSetting
    private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);


    @SettingSection("token")
    @MethodOrder(after = "enabled")
    @SpriteSlot(size = 32, x = 1)
    @ButtonSetting
    public void openRegisterPage(Setting setting) {
        new Timer().schedule(
            new TimerTask() {
                @Override
                public void run() {
                    Laby.labyAPI().minecraft().chatExecutor()
                        .openUrl("https://imageserver.pw/login", false);
                }
            }, 650);
    }

    @MethodOrder(after = "openRegisterPage")
    @SpriteSlot(size = 32, x = 2)
    @TextFieldSetting
    private final ConfigProperty<String> token = new ConfigProperty<>("");

    @Override
    public ConfigProperty<Boolean> enabled() {
        return enabled;
    }

    public String token() {
        return token.get();
    }
}
