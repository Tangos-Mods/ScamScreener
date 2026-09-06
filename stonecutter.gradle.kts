plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.17.19" apply false
    id("me.modmuss50.mod-publish-plugin") version "2.2.0" apply false
}

val minecraftVersions = providers.gradleProperty("minecraft.versions")
    .get()
    .split(',')
    .map(String::trim)
    .filter(String::isNotEmpty)

stonecutter active minecraftVersions.last()

stonecutter tasks {
    order("publishMods")
    order("publishModrinth")
    order("publishCurseforge")
}

stonecutter parameters {
    replacements {
        string(current.parsed < "26.2") {
            replace(".gui.setScreen(", ".setScreen(")
            replace(".gui.screen()", ".screen")
            replace(".gui.hud.isHidden()", ".options.hideGui")
        }
    }
}

fun registerAllVersionsPublishTask(taskName: String, description: String) {
    tasks.register(taskName) {
        group = "publishing"
        this.description = description
        dependsOn(subprojects.map { "${it.path}:$taskName" })
    }
}

fun registerPublishAliasTask(taskName: String, targetTaskName: String, description: String) {
    tasks.register(taskName) {
        group = "publishing"
        this.description = description
        dependsOn(targetTaskName)
    }
}

registerAllVersionsPublishTask(
    "publishMods",
    "Publishes all configured Minecraft versions to Modrinth and CurseForge."
)
registerAllVersionsPublishTask(
    "publishModrinth",
    "Publishes all configured Minecraft versions to Modrinth."
)
registerAllVersionsPublishTask(
    "publishCurseforge",
    "Publishes all configured Minecraft versions to CurseForge."
)
registerPublishAliasTask(
    "publishAllMods",
    "publishMods",
    "Alias for publishMods across all configured Minecraft versions."
)
registerPublishAliasTask(
    "publishAllModrinth",
    "publishModrinth",
    "Alias for publishModrinth across all configured Minecraft versions."
)
registerPublishAliasTask(
    "publishAllCurseforge",
    "publishCurseforge",
    "Alias for publishCurseforge across all configured Minecraft versions."
)
