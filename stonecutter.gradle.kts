plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom-remap") version "1.14-SNAPSHOT" apply false
    id("me.modmuss50.mod-publish-plugin") version "1.0.+" apply false
}

stonecutter active "1.21.11"

fun versionOrderKey(version: String): List<Int> =
    Regex("""\d+""").findAll(version).map { it.value.toInt() }.toList()

val publishTargets = layout.projectDirectory.dir("versions").asFile
    .listFiles()
    ?.asSequence()
    ?.filter { it.isDirectory && it.resolve("gradle.properties").isFile }
    ?.map { it.name }
    ?.sortedWith { left, right ->
        val leftKey = versionOrderKey(left)
        val rightKey = versionOrderKey(right)
        val maxSize = maxOf(leftKey.size, rightKey.size)
        for (index in 0 until maxSize) {
            val leftPart = leftKey.getOrElse(index) { 0 }
            val rightPart = rightKey.getOrElse(index) { 0 }
            if (leftPart != rightPart) {
                return@sortedWith leftPart.compareTo(rightPart)
            }
        }
        left.compareTo(right)
    }
    ?.toList()
    .orEmpty()

stonecutter tasks {
    // Publish older targets first so the newest target is uploaded last and stays on top.
    order("publishMods")
    order("publishModrinth")
    order("publishCurseforge")
}

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    constants["release"] = true
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String
}

tasks.register("publishAllMods") {
    group = "publishing"
    description = "Publishes all configured Minecraft targets to Modrinth and CurseForge."
    dependsOn(publishTargets.map { ":$it:publishMods" })
}

tasks.register("publishAllModrinth") {
    group = "publishing"
    description = "Publishes all configured Minecraft targets to Modrinth."
    dependsOn(publishTargets.map { ":$it:publishModrinth" })
}

tasks.register("publishAllCurseforge") {
    group = "publishing"
    description = "Publishes all configured Minecraft targets to CurseForge."
    dependsOn(publishTargets.map { ":$it:publishCurseforge" })
}
