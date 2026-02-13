$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$propertiesPath = Join-Path $repoRoot "gradle.properties"
$badgePath = Join-Path $repoRoot ".github/badges/version.json"

if (-not (Test-Path $propertiesPath)) {
	throw "gradle.properties not found: $propertiesPath"
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

$badgeDir = Split-Path -Parent $badgePath
if (-not (Test-Path $badgeDir)) {
	New-Item -ItemType Directory -Path $badgeDir | Out-Null
}

$payload = [ordered]@{
	schemaVersion = 1
	label = "version"
	message = $version
	color = "blue"
}

$json = $payload | ConvertTo-Json -Depth 4
Set-Content -Path $badgePath -Value $json -Encoding UTF8

Write-Host "Updated version badge: $version"
