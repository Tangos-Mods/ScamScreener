plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.15.5" apply false
    id("me.modmuss50.mod-publish-plugin") version "1.0.+" apply false
}

val publishedVersions = listOf("26.2")

stonecutter active "26.2"

stonecutter tasks {
    order("publishMods")
    order("publishModrinth")
    order("publishCurseforge")
}

fun registerAllVersionsPublishTask(taskName: String, description: String) {
    tasks.register(taskName) {
        group = "publishing"
        this.description = description
        dependsOn(publishedVersions.map { ":$it:$taskName" })
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
