plugins {
	id("net.fabricmc.fabric-loom-remap")
	id("maven-publish")
	id("me.modmuss50.mod-publish-plugin")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava = when {
	sc.current.parsed >= "1.20.6" -> JavaVersion.VERSION_21
	else -> JavaVersion.VERSION_17
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
	strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
	maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

dependencies {
	minecraft("com.mojang:minecraft:${stonecutter.current.version}")
	mappings(loom.officialMojangMappings())
	modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")

	modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
	modImplementation("maven.modrinth:modmenu:${property("deps.modmenu_version")}")

	compileOnly("org.projectlombok:lombok:1.18.36")
	annotationProcessor("org.projectlombok:lombok:1.18.36")
	testCompileOnly("org.projectlombok:lombok:1.18.36")
	testAnnotationProcessor("org.projectlombok:lombok:1.18.36")

	testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")

	modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.2")
	modRuntimeOnly("maven.modrinth:modmenu:${property("deps.modmenu_version")}")
}

loom {
	fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

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

	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

tasks {
	processResources {
		inputs.property("id", project.property("mod.id"))
		inputs.property("name", project.property("mod.name"))
		inputs.property("version", project.property("mod.version"))
		inputs.property("minecraft", project.property("mod.mc_dep"))
		inputs.property("fabric_api", project.property("deps.fabric_api"))
		inputs.property("modmenu", project.property("deps.modmenu_version"))
		inputs.property("fabricloader", project.property("deps.fabric_loader"))

		val props = mapOf(
			"id" to project.property("mod.id"),
			"name" to project.property("mod.name"),
			"version" to project.property("mod.version"),
			"minecraft" to project.property("mod.mc_dep"),
			"modmenu" to project.property("deps.modmenu_version"),
			"fabric_api" to project.property("deps.fabric_api"),
			"fabricloader" to project.property("deps.fabric_loader")
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

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
	testLogging {
		events("passed", "failed", "skipped")
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
	}
}

publishMods {
	file = tasks.remapJar.map { it.archiveFile.get() }
	additionalFiles.from(tasks.remapSourcesJar.map { it.archiveFile.get() })
	displayName = "${property("mod.name")} ${property("mod.version")} for ${stonecutter.current.version}"
	version = property("mod.version") as String
	changelog = rootProject.file("CHANGELOG.md").readText()
	type = STABLE
	modLoaders.add("fabric")

	dryRun = providers.environmentVariable("MODRINTH_TOKEN").getOrNull() == null

	modrinth {
		projectId = property("publish.modrinth") as String
		accessToken = providers.environmentVariable("MODRINTH_TOKEN")
		minecraftVersions.add(stonecutter.current.version)
		requires {
			slug = "P7dR8mSH" // Fabric API
		}
		optional {
			slug = "mOgUt4GM" // ModMenu
		}
	}

	/*
	curseforge {
		projectId = property("publish.curseforge") as String
		accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
		minecraftVersions.add(stonecutter.current.version)
		requires {
			slug = "fabric-api"
		}
		optional {
			slug = "modmenu"
		}
	}
	 */
}
