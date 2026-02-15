$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$modelPath = Join-Path $repoRoot "scripts/scam-screener-local-ai-model.json"
$versionPath = Join-Path $repoRoot "scripts/model-version.json"
$readmePath = Join-Path $repoRoot "README.md"

function Write-Utf8NoBom {
	param(
		[string]$Path,
		[string]$Value
	)
	$encoding = New-Object System.Text.UTF8Encoding($false)
	[System.IO.File]::WriteAllText($Path, $Value, $encoding)
}

if (-not (Test-Path $modelPath)) {
	throw "Model file not found: $modelPath"
}

$model = Get-Content $modelPath -Raw | ConvertFrom-Json
if ($null -eq $model.denseFeatureWeights) {
	throw "Model schema invalid: denseFeatureWeights missing"
}
if ($null -eq $model.tokenWeights) {
	throw "Model schema invalid: tokenWeights missing"
}
if ($null -eq $model.funnelHead) {
	throw "Model schema invalid: funnelHead missing"
}
if ($null -eq $model.funnelHead.denseFeatureWeights) {
	throw "Model schema invalid: funnelHead.denseFeatureWeights missing"
}

$version = 1
$schemaVersion = $null
if (Test-Path $versionPath) {
	try {
		$existing = Get-Content $versionPath -Raw | ConvertFrom-Json
		if ($null -ne $existing.version) {
			$version = [int]$existing.version + 1
		}
		if ($null -ne $existing.modelSchemaVersion) {
			$schemaVersion = [int]$existing.modelSchemaVersion
		}
	} catch {
		$version = 1
	}
}

$schemaFromModel = $null
if ($null -ne $model.PSObject.Properties["schemaVersion"]) {
	$schemaFromModel = [int]$model.schemaVersion
} elseif ($null -ne $model.version) {
	$schemaFromModel = [int]$model.version
}
if ($null -eq $schemaVersion) {
	$schemaVersion = $schemaFromModel
}
if ($null -eq $schemaVersion -or $schemaVersion -lt 9) {
	throw "Model schema invalid: version must be >= 9"
}

$releaseVersion = $version.ToString()
$model.version = [int]$releaseVersion
if ($null -ne $model.PSObject.Properties["modelVersion"]) {
	$model.PSObject.Properties.Remove("modelVersion")
}
$modelJson = $model | ConvertTo-Json -Depth 64
Write-Utf8NoBom -Path $modelPath -Value $modelJson

$modelBytes = [System.IO.File]::ReadAllBytes($modelPath)
$hasBom = $modelBytes.Length -ge 3 -and $modelBytes[0] -eq 0xEF -and $modelBytes[1] -eq 0xBB -and $modelBytes[2] -eq 0xBF
$modelText = [System.Text.Encoding]::UTF8.GetString($modelBytes)
$modelTextNoBom = $modelText.TrimStart([char]0xFEFF)
$canonicalText = ($modelTextNoBom -replace "`r`n", "`n") -replace "`r", "`n"
if ($hasBom) {
	$canonicalText = [char]0xFEFF + $canonicalText
}
$canonicalBytes = [System.Text.Encoding]::UTF8.GetBytes($canonicalText)
$sha = [System.Security.Cryptography.SHA256]::Create()
try {
	$hashBytes = $sha.ComputeHash($canonicalBytes)
} finally {
	$sha.Dispose()
}
$hash = -join ($hashBytes | ForEach-Object { $_.ToString("x2") })
$rawUrl = "https://raw.githubusercontent.com/Tangos-Mods/ScamScreener/main/scripts/scam-screener-local-ai-model.json"

$data = [ordered]@{
	version = $releaseVersion
	modelSchemaVersion = $schemaVersion
	sha256 = $hash
	url = $rawUrl
}

$json = $data | ConvertTo-Json -Depth 4
Write-Utf8NoBom -Path $versionPath -Value $json

if (Test-Path $readmePath) {
	$readme = Get-Content $readmePath -Raw
	$updatedReadme = [System.Text.RegularExpressions.Regex]::Replace(
		$readme,
		"https://img\\.shields\\.io/badge/AI%20Model-v[0-9A-Za-z._-]+-ff5555",
		"https://img.shields.io/badge/AI%20Model-v$releaseVersion-ff5555"
	)
	if ($updatedReadme -ne $readme) {
		Write-Utf8NoBom -Path $readmePath -Value $updatedReadme
	}
}

Write-Host "Updated model-version.json"
Write-Host "version=$($data.version)"
Write-Host "model.version=$($model.version)"
Write-Host "modelSchemaVersion=$($data.modelSchemaVersion)"
Write-Host "sha256=$($data.sha256)"
