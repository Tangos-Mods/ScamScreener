param(
	[string]$DataPath = "scripts/scam-screener-training-data.csv",
	[string]$OutPath = "scripts/scam-screener-local-ai-model.json",
	[switch]$BumpModel
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$trainerPath = Join-Path $repoRoot "scripts/train_local_ai.py"
$updateVersionPath = Join-Path $repoRoot "scripts/update-model-version.ps1"

function Resolve-RepoPath([string]$pathValue) {
	if ([System.IO.Path]::IsPathRooted($pathValue)) {
		return $pathValue
	}
	return Join-Path $repoRoot $pathValue
}

$resolvedDataPath = Resolve-RepoPath $DataPath
$resolvedOutPath = Resolve-RepoPath $OutPath

if (-not (Test-Path $trainerPath)) {
	throw "Training script not found: $trainerPath"
}
if (-not (Test-Path $resolvedDataPath)) {
	throw "Training data not found: $resolvedDataPath"
}

function Resolve-PythonCommand {
	$python = Get-Command python -ErrorAction SilentlyContinue
	if ($python) {
		return [pscustomobject]@{
			Exe = $python.Source
			Prefix = @()
		}
	}
	$py = Get-Command py -ErrorAction SilentlyContinue
	if ($py) {
		return [pscustomobject]@{
			Exe = $py.Source
			Prefix = @("-3")
		}
	}
	return $null
}

$pythonCommand = Resolve-PythonCommand
if ($null -eq $pythonCommand) {
	throw "Python not found. Install Python 3 and ensure 'python' or 'py' is in PATH."
}

$pythonExe = $pythonCommand.Exe
$pythonPrefixArgs = $pythonCommand.Prefix

& $pythonExe @pythonPrefixArgs "-c" "import sklearn"
if ($LASTEXITCODE -ne 0) {
	throw "Missing dependency 'scikit-learn'. Install with: pip install scikit-learn"
}

& $pythonExe @pythonPrefixArgs $trainerPath "--data" $resolvedDataPath "--out" $resolvedOutPath
if ($LASTEXITCODE -ne 0) {
	throw "Model training failed."
}

Write-Host "Model training completed."
Write-Host "data=$resolvedDataPath"
Write-Host "out=$resolvedOutPath"

if ($BumpModel) {
	if (-not (Test-Path $updateVersionPath)) {
		throw "Version update script not found: $updateVersionPath"
	}
	& $updateVersionPath
	if ($LASTEXITCODE -ne 0) {
		throw "Model version update failed."
	}
}
