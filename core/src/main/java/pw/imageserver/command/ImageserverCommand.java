package pw.imageserver.command;

import pw.imageserver.ImageserverAddon;
import pw.imageserver.api.UploadRequest;
import net.labymod.api.Laby;
import net.labymod.api.client.chat.command.Command;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.event.ClickEvent;
import net.labymod.api.client.component.event.HoverEvent;
import net.labymod.api.client.component.format.NamedTextColor;
import net.labymod.api.client.component.format.Style;
import net.labymod.api.client.component.format.TextDecoration;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ImageserverCommand extends Command {

    private final ImageserverAddon addon;
    private final Set<String> uploads = new HashSet<>();

    public ImageserverCommand(ImageserverAddon addon) {
        super("imageserver");
        this.addon = addon;
    }

    @Override
    public boolean execute(String prefix, String[] args) {
        if(addon.configuration().token().isBlank()) {
            displayMessage(ImageserverAddon.prefix.copy().append(Component.translatable("imageserver.errors.noToken", NamedTextColor.RED)));
            return true;
        }
        if(args.length < 1) {
            displayMessage(ImageserverAddon.prefix.copy().append(Component.translatable("imageserver.errors.file", NamedTextColor.RED)));
            return true;
        }
        File file = new File(System.getProperty("user.dir") + "/screenshots/" + args[0]);
        if(!file.exists()) {
            displayMessage(ImageserverAddon.prefix.copy().append(Component.translatable("imageserver.errors.file", NamedTextColor.RED)));
            return true;
        }
        if(uploads.contains(file.getName()) && addon.configuration().doubleUploads()) {
            if(args.length < 2 || !args[1].equalsIgnoreCase("force")) {
                displayMessage(ImageserverAddon.prefix.copy().append(
                    Component.translatable("imageserver.errors.alreadyUploaded", NamedTextColor.RED,
                        Component.translatable("imageserver.messages.uploadAnyway", Style.empty()
                                .color(NamedTextColor.AQUA)
                                .decorate(TextDecoration.UNDERLINED)
                                .hoverEvent(HoverEvent.showText(Component.translatable("imageserver.upload.hover", NamedTextColor.GREEN)))
                                .clickEvent(ClickEvent.runCommand(String.format("/%s %s force",prefix,file.getName())))
                        )
                    ))
                );
                return true;
            }
        }
        uploads.add(file.getName());
        displayMessage(ImageserverAddon.prefix.copy().append(Component.translatable("imageserver.messages.uploading", NamedTextColor.GRAY)));

        UploadRequest request = new UploadRequest(file, addon.configuration().token());
        request.sendAsyncRequest().thenAccept((response) -> {
            if(request.isSuccessful()) {
                Component copy = Component.translatable(
                    "imageserver.upload.copy",
                    Style.empty()
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD)
                        .hoverEvent(HoverEvent.showText(Component.translatable("imageserver.upload.hover").color(NamedTextColor.GREEN)))
                        .clickEvent(ClickEvent.copyToClipboard(request.getUploadLink()))
                );
                Component open = Component.translatable(
                    "imageserver.upload.open",
                    Style.empty()
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .hoverEvent(HoverEvent.showText(Component.translatable("imageserver.upload.hover").color(NamedTextColor.GREEN)))
                        .clickEvent(ClickEvent.openUrl(request.getUploadLink()))
                );
                Component component = Component.translatable(
                    "imageserver.messages.uploaded",
                    !request.getUploadLink().isBlank() ? copy : Component.text(""),
                    !request.getUploadLink().isBlank() ? open : Component.text("")
                ).color(NamedTextColor.GRAY);

                Laby.references().chatExecutor().displayClientMessage(ImageserverAddon.prefix.copy().append(component));
            } else {
                Laby.references().chatExecutor().displayClientMessage(ImageserverAddon.prefix.copy()
                    .append(Component.text(request.getError(),NamedTextColor.RED))
                );
            }
        }).exceptionally((e) -> {
            Laby.references().chatExecutor().displayClientMessage(ImageserverAddon.prefix.copy()
                .append(Component.text(e.getMessage(),NamedTextColor.RED))
            );
            return null;
        });
        return true;
    }
}
