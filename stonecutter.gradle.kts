plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.17.20" apply false
    id("me.modmuss50.mod-publish-plugin") version "2.2.0" apply false
}

// Keep this on the last entry of minecraft.versions. It has to be a literal so the
// "Set active project to X" / "Reset active project" tasks can rewrite it.
stonecutter active "26.3"

stonecutter tasks {
    order("publishMods")
    order("publishModrinth")
    order("publishCurseforge")
}

stonecutter parameters {
    replacements {
        // Replacements run in reverse for versions where the condition is false,
        // so the right-hand side must not match anything else in the source
        // (a bare ".screen" would also hit the gui.screen package).
        string(current.parsed < "26.2") {
            replace(".gui.setScreen(", ".setScreen(")
            replace("client.gui.screen()", "client.screen")
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
