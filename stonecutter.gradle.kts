plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") version "1.15.5" apply false
    id("me.modmuss50.mod-publish-plugin") version "1.0.+" apply false
}

stonecutter active "26.1"

stonecutter tasks {
    order("publishMods")
    order("publishModrinth")
    order("publishCurseforge")
}
