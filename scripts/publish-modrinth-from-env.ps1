param(
	[string]$EnvFile = ".env"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot

function Resolve-RepoPath([string]$pathValue) {
	if ([System.IO.Path]::IsPathRooted($pathValue)) {
		return $pathValue
	}
	return Join-Path $repoRoot $pathValue
}

function Import-DotEnv([string]$path) {
	if (-not (Test-Path $path)) {
		throw ".env not found. Create it from .env.example first. Missing: $path"
	}

	Get-Content $path | ForEach-Object {
		$line = $_.Trim()
		if ([string]::IsNullOrWhiteSpace($line)) {
			return
		}
		if ($line.StartsWith("#")) {
			return
		}
		$eq = $line.IndexOf("=")
		if ($eq -le 0) {
			return
		}
		$key = $line.Substring(0, $eq).Trim()
		$val = $line.Substring($eq + 1).Trim()
		if ($val.Length -ge 2 -and $val.StartsWith('"') -and $val.EndsWith('"')) {
			$val = $val.Substring(1, $val.Length - 2)
		}
		[System.Environment]::SetEnvironmentVariable($key, $val, "Process")
	}
}

$resolvedEnvFile = Resolve-RepoPath $EnvFile
Import-DotEnv $resolvedEnvFile

$gradleWrapper = Join-Path $repoRoot "gradlew.bat"
if (-not (Test-Path $gradleWrapper)) {
	throw "Gradle wrapper not found: $gradleWrapper"
}

$tasksOutput = & $gradleWrapper tasks --all --console=plain
if ($LASTEXITCODE -ne 0) {
	exit $LASTEXITCODE
}

$versions = $tasksOutput |
	Select-String -Pattern '^\s*([0-9]+\.[0-9]+\.[0-9]+):publishModrinth\b' |
	ForEach-Object { $_.Matches[0].Groups[1].Value } |
	Sort-Object { [Version]$_ } -Unique

if (-not $versions) {
	throw "No StoneCutter publishModrinth tasks found."
}

$publishTasks = $versions | ForEach-Object { ":{0}:publishModrinth" -f $_ }
Write-Host ("Publishing versions: " + ($versions -join ", "))

& $gradleWrapper @publishTasks
exit $LASTEXITCODE
