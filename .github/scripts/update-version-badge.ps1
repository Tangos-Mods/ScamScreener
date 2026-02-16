$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$propertiesPath = Join-Path $repoRoot "gradle.properties"
$modelVersionPath = Join-Path $repoRoot "scripts/model-version.json"
$versionBadgePath = Join-Path $repoRoot ".github/badges/version.json"
$aiBadgePath = Join-Path $repoRoot ".github/badges/ai-model.json"

function New-BadgePayload {
	param(
		[string]$Label,
		[string]$Message,
		[string]$Color
	)

	return [ordered]@{
		schemaVersion = 1
		label = $Label
		message = $Message
		color = $Color
	}
}

function Write-BadgeFile {
	param(
		[string]$Path,
		[hashtable]$Payload
	)

	$badgeDir = Split-Path -Parent $Path
	if (-not (Test-Path $badgeDir)) {
		New-Item -ItemType Directory -Path $badgeDir | Out-Null
	}

	$json = $Payload | ConvertTo-Json -Depth 4
	Set-Content -Path $Path -Value $json -Encoding UTF8
}

if (-not (Test-Path $propertiesPath)) {
	throw "gradle.properties not found: $propertiesPath"
}
if (-not (Test-Path $modelVersionPath)) {
	throw "model-version.json not found: $modelVersionPath"
}

$properties = Get-Content -Path $propertiesPath -Raw -Encoding UTF8
$match = [regex]::Match($properties, '(?m)^\s*mod\.version\s*=\s*([^\r\n#]+?)\s*$')
if (-not $match.Success) {
	throw "mod.version not found in gradle.properties"
}

$version = $match.Groups[1].Value.Trim()
if ([string]::IsNullOrWhiteSpace($version)) {
	throw "mod.version is empty in gradle.properties"
}

$modelVersionRaw = Get-Content -Path $modelVersionPath -Raw -Encoding UTF8
$modelVersionData = $modelVersionRaw | ConvertFrom-Json
$modelVersion = [string]$modelVersionData.version
if ([string]::IsNullOrWhiteSpace($modelVersion)) {
	throw "version is empty in model-version.json"
}
$modelVersion = $modelVersion.Trim()

Write-BadgeFile -Path $versionBadgePath -Payload (New-BadgePayload -Label "version" -Message $version -Color "blue")
Write-BadgeFile -Path $aiBadgePath -Payload (New-BadgePayload -Label "AI Model" -Message "v$modelVersion" -Color "5555ff")

Write-Host "Updated version badge: $version"
Write-Host "Updated AI model badge: v$modelVersion"
