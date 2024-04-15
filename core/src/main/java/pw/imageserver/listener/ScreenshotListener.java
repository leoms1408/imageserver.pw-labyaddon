package pw.imageserver.listener;

import pw.imageserver.ImageserverAddon;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.event.ClickEvent;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.component.format.Style;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.misc.WriteScreenshotEvent;

public class ScreenshotListener {

    @Subscribe
    public void onScreenshot(WriteScreenshotEvent event) {
        Component component = ImageserverAddon.prefix.copy().append(
            Component.translatable("imageserver.messages.upload",Style.empty()
                    .color(NamedTextColor.BLUE)
                    .clickEvent(ClickEvent.runCommand("/imageserver " + event.getDestination().getName())))
        );
        Laby.references().chatExecutor().displayClientMessage(component);
    }
}
