plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom-remap") version "1.15-SNAPSHOT" apply false
    id("me.modmuss50.mod-publish-plugin") version "1.0.+" apply false
}

stonecutter active "1.21.11"

// Make newer versions be published last
stonecutter tasks {
    order("publishModrinth")
    //order("publishCurseforge")
}

// See https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    constants["release"] = property("mod.id") != "template"
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String

    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }
    }
}

val isWindows = System.getProperty("os.name", "").lowercase().contains("win")
val powerShellExecutable = if (isWindows) "powershell.exe" else "pwsh"

fun registerPowerShellTask(taskName: String, taskDescription: String, vararg taskArguments: String) = tasks.register<Exec>(taskName) {
	group = "scamscreener"
	description = taskDescription
	workingDir = rootDir
	commandLine(powerShellExecutable, "-NoProfile", "-ExecutionPolicy", "Bypass", *taskArguments)
}

tasks.register("scamScreenerRefreshStonecutterSources") {
	group = "scamscreener"
	description = "Refreshes Stonecutter generated sources and recompiles Java for 1.21.9, 1.21.10 and 1.21.11."
	dependsOn(
		":1.21.9:stonecutterGenerate",
		":1.21.10:stonecutterGenerate",
		":1.21.11:stonecutterGenerate",
		":1.21.9:compileJava",
		":1.21.10:compileJava",
		":1.21.11:compileJava"
	)
}

registerPowerShellTask(
	"scamScreenerUpdateModelVersion",
	"Updates scripts/model-version.json using scripts/update-model-version.ps1.",
	"-File", "${rootDir}/scripts/update-model-version.ps1"
)

registerPowerShellTask(
	"scamScreenerPublishModrinth",
	"Publishes all Stonecutter versions to Modrinth using .env credentials.",
	"-File", "${rootDir}/scripts/publish-modrinth-from-env.ps1",
	"-EnvFile", "${rootDir}/.env"
)

registerPowerShellTask(
	"scamScreenerPublishCurseForge",
	"Publishes all Stonecutter versions to CurseForge using .env credentials.",
	"-File", "${rootDir}/scripts/publish-modrinth-from-env.ps1",
	"-EnvFile", "${rootDir}/.env",
	"-PublishTaskName", "publishCurseforge"
)

registerPowerShellTask(
	"scamScreenerTrainAiModel",
	"Trains the local AI model from scripts/scam-screener-training-data.csv.",
	"-File", "${rootDir}/scripts/train-model.ps1",
	"-DataPath", "scripts/scam-screener-training-data.csv",
	"-OutPath", "scripts/scam-screener-local-ai-model.json"
)

tasks.named("scamScreenerUpdateModelVersion") {
	mustRunAfter("scamScreenerTrainAiModel")
}

tasks.register("scamScreenerTrainAndUpdateAiModel") {
	group = "scamscreener"
	description = "Trains the local AI model and updates scripts/model-version.json."
	dependsOn("scamScreenerTrainAiModel", "scamScreenerUpdateModelVersion")
	doLast {
		println("Trained and updated AI model: SUCCESS")
	}
}
