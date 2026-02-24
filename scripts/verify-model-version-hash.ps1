$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$modelPath = Join-Path $repoRoot "scripts/scam-screener-local-ai-model.json"
$versionPath = Join-Path $repoRoot "scripts/model-version.json"

if (-not (Test-Path $modelPath)) {
	throw "Model file not found: $modelPath"
}
if (-not (Test-Path $versionPath)) {
	throw "Version file not found: $versionPath"
}

$version = Get-Content $versionPath -Raw | ConvertFrom-Json
if ($null -eq $version.sha256 -or [string]::IsNullOrWhiteSpace([string]$version.sha256)) {
	throw "model-version.json is missing sha256"
}
if ($null -eq $version.version -or [string]::IsNullOrWhiteSpace([string]$version.version)) {
	throw "model-version.json is missing version"
}

$model = Get-Content $modelPath -Raw | ConvertFrom-Json
if ($null -eq $model.version) {
	throw "Model file is missing version"
}

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
	$actualHash = -join ($sha.ComputeHash($canonicalBytes) | ForEach-Object { $_.ToString("x2") })
} finally {
	$sha.Dispose()
}

$expectedHash = ([string]$version.sha256).Trim().ToLowerInvariant()
if ($actualHash -ne $expectedHash) {
	throw "SHA mismatch. expected=$expectedHash actual=$actualHash"
}

$modelVersion = [int]$model.version
$versionFileVersion = [int]$version.version
if ($modelVersion -ne $versionFileVersion) {
	throw "Version mismatch. model.version=$modelVersion model-version.version=$versionFileVersion"
}

Write-Host "Model hash/version verification passed."
Write-Host "version=$versionFileVersion"
Write-Host "sha256=$actualHash"
