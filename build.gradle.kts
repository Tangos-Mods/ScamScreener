plugins {
    id("net.fabricmc.fabric-loom-remap")

    // `maven-publish`
    id("me.modmuss50.mod-publish-plugin")
}

fun loadDotEnv(file: File): Map<String, String> {
    if (!file.isFile) {
        return emptyMap()
    }

    val values = mutableMapOf<String, String>()
    file.forEachLine { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) {
            return@forEachLine
        }

        val separator = line.indexOf('=')
        if (separator <= 0) {
            return@forEachLine
        }

        val key = line.substring(0, separator).trim()
        if (key.isBlank()) {
            return@forEachLine
        }

        var value = line.substring(separator + 1).trim()
        if (value.length >= 2) {
            val quoted = (value.startsWith('"') && value.endsWith('"'))
                || (value.startsWith('\'') && value.endsWith('\''))
            if (quoted) {
                value = value.substring(1, value.length - 1)
            }
        }

        values[key] = value
    }

    return values
}

val dotEnvValues = loadDotEnv(rootProject.file(".env"))

fun resolveSecret(name: String): String? {
    val fromEnvironment = providers.environmentVariable(name).orNull?.trim().orEmpty()
    if (fromEnvironment.isNotEmpty()) {
        return fromEnvironment
    }

    val fromGradleProperty = providers.gradleProperty(name).orNull?.trim().orEmpty()
    if (fromGradleProperty.isNotEmpty()) {
        return fromGradleProperty
    }

    val fromDotEnv = dotEnvValues[name]?.trim().orEmpty()
    return fromDotEnv.ifEmpty { null }
}

val modrinthToken = resolveSecret("MODRINTH_TOKEN")
val curseforgeToken = resolveSecret("CURSEFORGE_TOKEN")

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava = when {
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

val yarnMappings = when (sc.current.version) {
    "1.21.10" -> "1.21.10+build.3"
    "1.21.11" -> "1.21.11+build.4"
    else -> error("No Yarn mappings configured for ${sc.current.version}. Add a matching Yarn version before building this target.")
}

repositories {
    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {
    /**
     * Fetches only the required Fabric API modules to not waste time downloading all of them for each version.
     * @see <a href="https://github.com/FabricMC/fabric">List of Fabric API modules</a>
     */
    fun fapi(vararg modules: String) {
        for (it in modules) modImplementation(fabricApi.module(it, property("deps.fabric_api") as String))
    }

    minecraft("com.mojang:minecraft:${sc.current.version}")
    mappings("net.fabricmc:yarn:${yarnMappings}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    modImplementation("maven.modrinth:modmenu:${property("deps.modmenu_version")}")

    fapi(
        "fabric-command-api-v2",
        "fabric-lifecycle-events-v1",
        "fabric-networking-api-v1",
        "fabric-resource-loader-v0",
        "fabric-content-registries-v0",
        "fabric-message-api-v1"
    )
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection
    accessWidenerPath = rootProject.file("src/main/resources/scamscreener.accesswidener")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDir = "../../run" // Shares the run directory between versions
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "failed", "skipped")
        }
    }

    register<Test>("manualChatInspection") {
        group = "verification"
        description = "Runs the manual chat inspection harness and shows line classification output."
        useJUnitPlatform()
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        filter {
            includeTestsMatching("eu.tango.scamscreener.chat.ManualChatInspectionTest")
        }
        testLogging {
            events("passed", "failed", "skipped")
            showStandardStreams = true
        }
    }

    processResources {
        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("mod.mc_dep"))

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep")
        )

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile }, remapSourcesJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

// Publishes builds to Modrinth and Curseforge with player-facing changelog from MODRINTH.md
publishMods {
    file = tasks.remapJar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.remapSourcesJar.map { it.archiveFile.get() })
    displayName = "${property("mod.name")} ${property("mod.version")} for ${property("mod.mc_title")}"
    version = property("mod.version") as String
    changelog = rootProject.file("MODRINTH.md").readText()
    type = STABLE
    modLoaders.add("fabric")

    dryRun = modrinthToken == null || curseforgeToken == null

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = providers.provider { modrinthToken.orEmpty() }
        minecraftVersions.addAll(property("mod.mc_targets").toString().split(' '))
        requires {
            slug = "fabric-api"
        }
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = providers.provider { curseforgeToken.orEmpty() }
        minecraftVersions.addAll(property("mod.mc_targets").toString().split(' '))
        requires {
            slug = "fabric-api"
        }
    }
}

/*
// Publishes builds to a maven repository under `eu.tango.scamscreener:scamscreener:0.1.0+mc`
publishing {
    repositories {
        maven("https://maven.example.com/releases") {
            name = "myMaven"
            // To authenticate, create `myMavenUsername` and `myMavenPassword` properties in your Gradle home properties.
            // See https://stonecutter.kikugie.dev/wiki/tips/properties#defining-properties
            credentials(PasswordCredentials::class.java)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "${property("mod.group")}.${property("mod.id")}"
            artifactId = property("mod.id") as String
            version = project.version

            from(components["java"])
        }
    }
}
 */
