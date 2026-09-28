import net.labymod.labygradle.common.extension.model.labymod.ReleaseChannel
import net.labymod.labygradle.common.internal.labymod.addon.model.AddonMeta

plugins {
    id("net.labymod.labygradle")
    id("net.labymod.labygradle.addon")
    id("com.diffplug.spotless") version ("8.10.3")
}

spotless {
    lineEndings = com.diffplug.spotless.LineEnding.UNIX

    // Checks that every Java file starts with the license header (spotlessApply adds it)
    format("licenseHeader") {
        target("**/src/**/*.java")
        licenseHeaderFile(rootProject.file("gradle/license-header.txt"), "package ")
    }
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
        version = rootProject.version.toString()
    }

    minecraft {
        registerVersion(versions.toTypedArray()) {
            runs {
                getByName("client") {
                    // When the property is set to true, you can log in with a Minecraft account
                    // devLogin = true
                }
            }
        }
    }
}

subprojects {
    plugins.apply("net.labymod.labygradle")
    plugins.apply("net.labymod.labygradle.addon")

    group = rootProject.group
    version = rootProject.version

    extensions.findByType(JavaPluginExtension::class.java)?.apply {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
