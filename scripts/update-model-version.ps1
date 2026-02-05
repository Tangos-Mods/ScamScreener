$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$modelPath = Join-Path $repoRoot "scripts/scam-screener-local-ai-model.json"
$versionPath = Join-Path $repoRoot "scripts/model-version.json"

if (-not (Test-Path $modelPath)) {
	throw "Model file not found: $modelPath"
}

$hash = (Get-FileHash -Algorithm SHA256 $modelPath).Hash.ToLowerInvariant()
$rawUrl = "https://raw.githubusercontent.com/Tangos-Mods/ScamScreener/main/scripts/scam-screener-local-ai-model.json"

$version = 1
if (Test-Path $versionPath) {
	try {
		$existing = Get-Content $versionPath -Raw | ConvertFrom-Json
		if ($null -ne $existing.version) {
			$version = [int]$existing.version + 1
		}
	} catch {
		$version = 1
	}
}

$data = [ordered]@{
	version = $version.ToString()
	sha256 = $hash
	url = $rawUrl
}

$json = $data | ConvertTo-Json -Depth 4
Set-Content -Path $versionPath -Value $json -Encoding UTF8

Write-Host "Updated model-version.json"
Write-Host "version=$($data.version)"
Write-Host "sha256=$($data.sha256)"
