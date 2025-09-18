import net.labymod.labygradle.common.extension.model.labymod.ReleaseChannel
import net.labymod.labygradle.common.internal.labymod.addon.model.AddonMeta

plugins {
    id("net.labymod.labygradle")
    id("net.labymod.labygradle.addon")
    id("org.cadixdev.licenser") version ("0.6.1")
}

val versions = providers.gradleProperty("net.labymod.minecraft-versions").get().split(";")

group = "pw.imageserver"
version = providers.environmentVariable("VERSION").getOrElse("2.0.0")

labyMod {
    defaultPackageName = "pw.imageserver.uploader"
    addonInfo {
        namespace = "imageserver"
        displayName = "imageserver.pw Uploader"
        author = "leoms1408"
        description = "Upload your screenshots directly to imageserver.pw"
        minecraftVersion = "*"
        version = System.getenv().getOrDefault("VERSION", "2.0.0")
    }

    minecraft {
        registerVersion(versions.toTypedArray()) {
            runs {
            }
        }
    }
}

subprojects {
    plugins.apply("net.labymod.labygradle")
    plugins.apply("net.labymod.labygradle.addon")
    plugins.apply("org.cadixdev.licenser")
    license {
        header(rootProject.file("gradle/LICENSE-HEADER.txt"))
        newLine.set(true)
    }
}
