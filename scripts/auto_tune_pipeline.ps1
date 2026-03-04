param(
    [string]$TrainingDataDir = "trainingdata",
    [string]$RulesFile = "run/config/scamscreener/rules.json",
    [string]$OutputFile = "trainingdata/rules.autotuned.json",
    [int]$MaxStep = 3,
    [switch]$Apply
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Normalize-IdSuffix {
    param([string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value)) {
        return ""
    }

    $builder = New-Object System.Text.StringBuilder
    $lastWasSeparator = $false
    foreach ($char in $Value.ToLowerInvariant().ToCharArray()) {
        if (($char -ge "a" -and $char -le "z") -or ($char -ge "0" -and $char -le "9")) {
            [void]$builder.Append($char)
            $lastWasSeparator = $false
            continue
        }

        if (-not $lastWasSeparator) {
            [void]$builder.Append("_")
            $lastWasSeparator = $true
        }
    }

    $normalized = $builder.ToString().Trim("_")
    return $normalized
}

function Clamp-Int {
    param(
        [int]$Value,
        [int]$Min,
        [int]$Max
    )

    if ($Value -lt $Min) {
        return $Min
    }
    if ($Value -gt $Max) {
        return $Max
    }

    return $Value
}

function Get-OrCreate-Section {
    param(
        [pscustomobject]$Root,
        [string]$SectionName
    )

    if ($null -eq $Root) {
        throw "Rules root object is null."
    }

    $hasSection = $Root.PSObject.Properties.Match($SectionName).Count -gt 0
    if (-not $hasSection -or $null -eq $Root.$SectionName) {
        $Root | Add-Member -NotePropertyName $SectionName -NotePropertyValue ([pscustomobject]@{}) -Force
    }

    return [pscustomobject]$Root.$SectionName
}

function Get-OrCreate-Int {
    param(
        [pscustomobject]$Object,
        [string]$PropertyName,
        [int]$DefaultValue
    )

    $hasProperty = $Object.PSObject.Properties.Match($PropertyName).Count -gt 0
    if (-not $hasProperty -or $null -eq $Object.$PropertyName) {
        $Object | Add-Member -NotePropertyName $PropertyName -NotePropertyValue $DefaultValue -Force
        return $DefaultValue
    }

    try {
        return [int]$Object.$PropertyName
    } catch {
        $Object | Add-Member -NotePropertyName $PropertyName -NotePropertyValue $DefaultValue -Force
        return $DefaultValue
    }
}

function Add-CalibrationStat {
    param(
        [hashtable]$Stats,
        [string]$MappingId,
        [int]$WeightDeltaHint
    )

    if (-not $Stats.ContainsKey($MappingId)) {
        $Stats[$MappingId] = [pscustomobject]@{
            total = 0
            count = 0
            pos = 0
            neg = 0
        }
    }

    $entry = $Stats[$MappingId]
    $entry.total += $WeightDeltaHint
    $entry.count += 1
    if ($WeightDeltaHint -gt 0) {
        $entry.pos += 1
    } elseif ($WeightDeltaHint -lt 0) {
        $entry.neg += 1
    }
}

function Resolve-Step {
    param(
        [pscustomobject]$Stat,
        [int]$MaxStepValue
    )

    $total = [int]$Stat.total
    $absoluteTotal = [Math]::Abs($total)
    $step = 0
    if ($absoluteTotal -ge 24) {
        $step = 4
    } elseif ($absoluteTotal -ge 12) {
        $step = 3
    } elseif ($absoluteTotal -ge 5) {
        $step = 2
    } elseif ($absoluteTotal -ge 2) {
        $step = 1
    }

    if ($step -le 0) {
        return 0
    }

    $positiveCount = [int]$Stat.pos
    $negativeCount = [int]$Stat.neg
    if ($positiveCount -gt 0 -and $negativeCount -gt 0) {
        $ratio = [double]([Math]::Min($positiveCount, $negativeCount)) / [double]([Math]::Max($positiveCount, $negativeCount))
        if ($ratio -ge 0.75) {
            return 0
        }
        if ($ratio -ge 0.45 -and $step -gt 1) {
            $step -= 1
        }
    }

    if ($step -gt $MaxStepValue) {
        $step = $MaxStepValue
    }

    if ($step -le 0) {
        return 0
    }

    if ($total -lt 0) {
        return -$step
    }

    return $step
}

function Register-Change {
    param(
        [System.Collections.Generic.List[object]]$Changes,
        [string]$MappingId,
        [string]$Field,
        [int]$Before,
        [int]$After
    )

    $Changes.Add([pscustomobject]@{
            mappingId = $MappingId
            field = $Field
            before = $Before
            after = $After
            delta = $After - $Before
        })
}

function Apply-IntAdjustment {
    param(
        [pscustomobject]$Rules,
        [string]$SectionName,
        [string]$FieldName,
        [int]$Step,
        [int]$Scale,
        [int]$MinValue,
        [int]$MaxValue,
        [int]$DefaultValue,
        [string]$MappingId,
        [System.Collections.Generic.List[object]]$Changes
    )

    if ($Step -eq 0) {
        return $false
    }

    $section = Get-OrCreate-Section -Root $Rules -SectionName $SectionName
    $before = Get-OrCreate-Int -Object $section -PropertyName $FieldName -DefaultValue $DefaultValue
    $rawAfter = $before + ($Step * $Scale)
    $after = Clamp-Int -Value $rawAfter -Min $MinValue -Max $MaxValue
    if ($after -eq $before) {
        return $false
    }

    $section | Add-Member -NotePropertyName $FieldName -NotePropertyValue $after -Force
    Register-Change -Changes $Changes -MappingId $MappingId -Field "$SectionName.$FieldName" -Before $before -After $after
    return $true
}

function Apply-SimilarityScoreAdjustment {
    param(
        [pscustomobject]$Rules,
        [string]$ReasonId,
        [int]$Step,
        [string]$MappingId,
        [System.Collections.Generic.List[object]]$Changes
    )

    if ($Step -eq 0) {
        return 0
    }

    $similarityStage = Get-OrCreate-Section -Root $Rules -SectionName "similarityStage"
    $hasPhrases = $similarityStage.PSObject.Properties.Match("phrases").Count -gt 0
    if (-not $hasPhrases -or $null -eq $similarityStage.phrases) {
        $similarityStage | Add-Member -NotePropertyName "phrases" -NotePropertyValue @() -Force
    }

    $matched = 0
    foreach ($phrase in @($similarityStage.phrases)) {
        if ($null -eq $phrase) {
            continue
        }

        $categoryValue = ""
        if ($phrase.PSObject.Properties.Match("category").Count -gt 0 -and $null -ne $phrase.category) {
            $categoryValue = [string]$phrase.category
        }

        $normalizedCategory = Normalize-IdSuffix -Value $categoryValue
        if ([string]::IsNullOrWhiteSpace($normalizedCategory)) {
            continue
        }

        if ("similarity.$normalizedCategory" -ne $ReasonId) {
            continue
        }

        $before = 0
        if ($phrase.PSObject.Properties.Match("score").Count -gt 0 -and $null -ne $phrase.score) {
            $before = [int]$phrase.score
        } else {
            $phrase | Add-Member -NotePropertyName "score" -NotePropertyValue 0 -Force
        }

        $after = Clamp-Int -Value ($before + $Step) -Min 0 -Max 100
        if ($after -eq $before) {
            continue
        }

        $phrase.score = $after
        Register-Change -Changes $Changes -MappingId $MappingId -Field "similarityStage.phrases[$matched].score" -Before $before -After $after
        $matched += 1
    }

    return $matched
}

if ([string]::IsNullOrWhiteSpace($TrainingDataDir)) {
    throw "TrainingDataDir must not be blank."
}
if ([string]::IsNullOrWhiteSpace($RulesFile)) {
    throw "RulesFile must not be blank."
}
if ([string]::IsNullOrWhiteSpace($OutputFile)) {
    throw "OutputFile must not be blank."
}

$trainingDataPath = (Resolve-Path -Path $TrainingDataDir -ErrorAction SilentlyContinue)
if ($null -eq $trainingDataPath) {
    throw "Training data directory not found: $TrainingDataDir"
}

if (-not (Test-Path -Path $RulesFile -PathType Leaf)) {
    throw "Rules file not found: $RulesFile"
}

$trainingCaseFiles = @(Get-ChildItem -Path $trainingDataPath.Path -Recurse -File -Filter "training-cases-v2.jsonl")
$legacyCalibrationFiles = @(Get-ChildItem -Path $trainingDataPath.Path -Recurse -File -Filter "fixed-stage-calibrations-v2.jsonl")
if ($trainingCaseFiles.Count -eq 0 -and $legacyCalibrationFiles.Count -eq 0) {
    throw "No training-cases-v2.jsonl (or legacy fixed-stage-calibrations-v2.jsonl) files found under $($trainingDataPath.Path)."
}

$statsByMapping = @{}
$lineCount = 0
$usedLineCount = 0
$sourceMode = ""
if ($trainingCaseFiles.Count -gt 0) {
    $sourceMode = "training_case_v2"
    foreach ($trainingCaseFile in $trainingCaseFiles) {
        foreach ($line in Get-Content -Path $trainingCaseFile.FullName) {
            $lineCount += 1
            $trimmed = $line.Trim()
            if ([string]::IsNullOrWhiteSpace($trimmed)) {
                continue
            }

            $row = $trimmed | ConvertFrom-Json
            if ($null -eq $row) {
                continue
            }
            if ($row.PSObject.Properties.Match("format").Count -gt 0 -and [string]$row.format -ne "training_case_v2") {
                continue
            }
            if ($row.PSObject.Properties.Match("supervision").Count -eq 0 -or $null -eq $row.supervision) {
                continue
            }

            $supervision = $row.supervision
            if ($supervision.PSObject.Properties.Match("fixedStageCalibrations").Count -eq 0 -or $null -eq $supervision.fixedStageCalibrations) {
                continue
            }

            foreach ($calibration in @($supervision.fixedStageCalibrations)) {
                if ($null -eq $calibration) {
                    continue
                }

                $mappingId = ""
                if ($calibration.PSObject.Properties.Match("mappingId").Count -gt 0 -and $null -ne $calibration.mappingId) {
                    $mappingId = [string]$calibration.mappingId
                }
                if ([string]::IsNullOrWhiteSpace($mappingId)) {
                    continue
                }

                $weightDeltaHint = 0
                if ($calibration.PSObject.Properties.Match("weightDeltaHint").Count -gt 0 -and $null -ne $calibration.weightDeltaHint) {
                    $weightDeltaHint = [int]$calibration.weightDeltaHint
                }
                if ($weightDeltaHint -eq 0) {
                    continue
                }

                Add-CalibrationStat -Stats $statsByMapping -MappingId $mappingId -WeightDeltaHint $weightDeltaHint
                $usedLineCount += 1
            }
        }
    }
} else {
    $sourceMode = "legacy_fixed_stage_calibration_v2"
    foreach ($calibrationFile in $legacyCalibrationFiles) {
        foreach ($line in Get-Content -Path $calibrationFile.FullName) {
            $lineCount += 1
            $trimmed = $line.Trim()
            if ([string]::IsNullOrWhiteSpace($trimmed)) {
                continue
            }

            $row = $trimmed | ConvertFrom-Json
            if ($null -eq $row) {
                continue
            }
            if ($row.PSObject.Properties.Match("format").Count -gt 0 -and [string]$row.format -ne "fixed_stage_calibration_v2") {
                continue
            }
            if ($row.PSObject.Properties.Match("calibration").Count -eq 0 -or $null -eq $row.calibration) {
                continue
            }

            $calibration = $row.calibration
            $mappingId = ""
            if ($calibration.PSObject.Properties.Match("mappingId").Count -gt 0 -and $null -ne $calibration.mappingId) {
                $mappingId = [string]$calibration.mappingId
            }
            if ([string]::IsNullOrWhiteSpace($mappingId)) {
                continue
            }

            $weightDeltaHint = 0
            if ($calibration.PSObject.Properties.Match("weightDeltaHint").Count -gt 0 -and $null -ne $calibration.weightDeltaHint) {
                $weightDeltaHint = [int]$calibration.weightDeltaHint
            }
            if ($weightDeltaHint -eq 0) {
                continue
            }

            Add-CalibrationStat -Stats $statsByMapping -MappingId $mappingId -WeightDeltaHint $weightDeltaHint
            $usedLineCount += 1
        }
    }
}

if ($statsByMapping.Count -eq 0) {
    throw "No usable calibration lines found in input files."
}

$sourceFileCount = if ($sourceMode -eq "training_case_v2") { $trainingCaseFiles.Count } else { $legacyCalibrationFiles.Count }

$rules = Get-Content -Path $RulesFile -Raw | ConvertFrom-Json
if ($null -eq $rules) {
    throw "Failed to parse rules JSON from $RulesFile"
}

$changes = New-Object "System.Collections.Generic.List[object]"
$unhandledMappings = New-Object "System.Collections.Generic.HashSet[string]"

foreach ($mappingEntry in ($statsByMapping.GetEnumerator() | Sort-Object Name)) {
    $mappingId = [string]$mappingEntry.Key
    $step = Resolve-Step -Stat $mappingEntry.Value -MaxStepValue $MaxStep
    if ($step -eq 0) {
        continue
    }

    $handled = $false
    switch ($mappingId) {
        "stage.rule::rule.suspicious_link" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "ruleStage" -FieldName "suspiciousLinkScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 20 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.rule::rule.external_platform" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "ruleStage" -FieldName "externalPlatformScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 15 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.rule::rule.upfront_payment" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "ruleStage" -FieldName "upfrontPaymentScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 25 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.rule::rule.account_data" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "ruleStage" -FieldName "accountDataScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 35 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.rule::rule.too_good" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "ruleStage" -FieldName "tooGoodScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 15 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.rule::rule.coercion_threat" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "ruleStage" -FieldName "coercionThreatScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 20 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.rule::rule.middleman_claim" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "ruleStage" -FieldName "middlemanClaimScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 15 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.rule::rule.proof_bait" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "ruleStage" -FieldName "proofBaitScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 10 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.rule::rule.urgency" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "ruleStage" -FieldName "urgencyScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 10 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.rule::rule.trust" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "ruleStage" -FieldName "trustScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 10 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.rule::rule.discord_handle" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "ruleStage" -FieldName "discordHandleScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 50 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.rule::rule.link_redirect_combo" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "ruleStage" -FieldName "linkRedirectComboScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 10 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.rule::rule.trust_payment_combo" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "ruleStage" -FieldName "trustPaymentComboScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 15 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.rule::rule.urgency_account_combo" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "ruleStage" -FieldName "urgencyAccountComboScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 15 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.rule::rule.middleman_proof_combo" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "ruleStage" -FieldName "middlemanProofComboScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 10 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.behavior::behavior.repeated_message" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "behaviorStage" -FieldName "repeatedMessageThreshold" -Step $step -Scale -1 -MinValue 1 -MaxValue 12 -DefaultValue 1 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.behavior::behavior.burst_contact" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "behaviorStage" -FieldName "burstContactThreshold" -Step $step -Scale -1 -MinValue 1 -MaxValue 20 -DefaultValue 3 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.behavior::behavior.combo_repeated_burst" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "behaviorStage" -FieldName "comboBonusMinimum" -Step $step -Scale 1 -MinValue 0 -MaxValue 8 -DefaultValue 0 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.trend::trend.single_cross_sender_repeat" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "trendStage" -FieldName "singleSenderRepeatScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 10 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.trend::trend.multi_sender_wave" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "trendStage" -FieldName "multiSenderWaveScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 20 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.trend::trend.wave_escalation" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "trendStage" -FieldName "escalationBonusMinimum" -Step $step -Scale 1 -MinValue 1 -MaxValue 20 -DefaultValue 1 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.funnel::funnel.external_after_contact" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "funnelStage" -FieldName "externalAfterContactScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 8 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.funnel::funnel.external_after_trust" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "funnelStage" -FieldName "trustBridgeBonusMinimum" -Step $step -Scale 1 -MinValue 1 -MaxValue 20 -DefaultValue 1 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.funnel::funnel.payment_after_external" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "funnelStage" -FieldName "paymentAfterExternalScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 18 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.funnel::funnel.payment_after_trust" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "funnelStage" -FieldName "paymentAfterTrustScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 10 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.funnel::funnel.account_after_external" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "funnelStage" -FieldName "accountAfterExternalScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 22 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.funnel::funnel.account_after_trust" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "funnelStage" -FieldName "accountAfterTrustScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 14 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.funnel::funnel.full_chain" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "funnelStage" -FieldName "fullChainBonusScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 100 -DefaultValue 10 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.context::context.signal_blend" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "contextStage" -FieldName "signalBlendScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 50 -DefaultValue 2 -MappingId $mappingId -Changes $changes
            break
        }
        "stage.context::context.escalation" {
            $handled = Apply-IntAdjustment -Rules $rules -SectionName "contextStage" -FieldName "escalationBonusScore" -Step $step -Scale 1 -MinValue 0 -MaxValue 50 -DefaultValue 1 -MappingId $mappingId -Changes $changes
            break
        }
        default {
            if ($mappingId.StartsWith("stage.similarity::")) {
                $reasonId = $mappingId.Substring("stage.similarity::".Length).ToLowerInvariant()
                $matchedCount = Apply-SimilarityScoreAdjustment -Rules $rules -ReasonId $reasonId -Step $step -MappingId $mappingId -Changes $changes
                $handled = $matchedCount -gt 0
            }
        }
    }

    if (-not $handled) {
        [void]$unhandledMappings.Add($mappingId)
    }
}

$outputDirectory = Split-Path -Path $OutputFile -Parent
if (-not [string]::IsNullOrWhiteSpace($outputDirectory)) {
    [void](New-Item -Path $outputDirectory -ItemType Directory -Force)
}

$serialized = $rules | ConvertTo-Json -Depth 100
Set-Content -Path $OutputFile -Value $serialized -Encoding UTF8

if ($Apply) {
    Set-Content -Path $RulesFile -Value $serialized -Encoding UTF8
}

Write-Host "Auto-tune complete."
Write-Host ("Training data directory: " + $trainingDataPath.Path)
Write-Host ("Source mode: " + $sourceMode)
Write-Host ("Input files: " + $sourceFileCount)
Write-Host ("Parsed lines: " + $lineCount + " | Used calibration lines: " + $usedLineCount)
Write-Host ("Distinct mapping ids: " + $statsByMapping.Count)
Write-Host ("Adjusted fields: " + $changes.Count)
Write-Host ("Output file: " + (Resolve-Path -Path $OutputFile).Path)
if ($Apply) {
    Write-Host ("Applied to rules file: " + (Resolve-Path -Path $RulesFile).Path)
} else {
    Write-Host "Dry run mode active. Use -Apply to overwrite the rules file."
}

if ($changes.Count -gt 0) {
    Write-Host ""
    Write-Host "Top changes:"
    foreach ($change in ($changes | Sort-Object -Property @{Expression = { [Math]::Abs($_.delta) }; Descending = $true } | Select-Object -First 20)) {
        Write-Host ("- " + $change.field + " (" + $change.mappingId + "): " + $change.before + " -> " + $change.after + " (delta " + $change.delta + ")")
    }
}

if ($unhandledMappings.Count -gt 0) {
    Write-Host ""
    Write-Host "Unhandled mapping ids:"
    foreach ($mappingId in ($unhandledMappings | Sort-Object)) {
        Write-Host ("- " + $mappingId)
    }
}
