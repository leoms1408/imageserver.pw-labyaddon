package pw.imageserver;

import pw.imageserver.listener.ScreenshotListener;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.models.addon.annotation.AddonMain;
import pw.imageserver.command.ImageserverCommand;

@AddonMain
public class ImageserverAddon extends LabyAddon<ImageserverConfig> {


    public static final Component prefix = Component.empty()
        .append(Component.text("[", NamedTextColor.GRAY))
        .append(Component.text("imageserver", NamedTextColor.GOLD))
        .append(Component.text("] ", NamedTextColor.GRAY));

    @Override
    protected void enable() {
        registerSettingCategory();
        registerCommand(new ImageserverCommand(this));
        registerListener(new ScreenshotListener());
    }

    @Override
    protected Class<? extends ImageserverConfig> configurationClass() {
        return ImageserverConfig.class;
    }
}
