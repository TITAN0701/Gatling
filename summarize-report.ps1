$ErrorActionPreference = "Stop"

$lastRunPath = Join-Path $PSScriptRoot "target/gatling/lastRun.txt"
if (-not (Test-Path $lastRunPath)) {
  Write-Error "lastRun.txt not found. Run Gatling first."
}

$runDir = (Get-Content -Raw $lastRunPath).Trim()
$indexPath = Join-Path $PSScriptRoot ("target/gatling/{0}/index.html" -f $runDir)
if (-not (Test-Path $indexPath)) {
  Write-Error "index.html not found: $indexPath"
}

$content = Get-Content -Raw $indexPath

function Get-FirstMatch([string]$pattern) {
  $m = [regex]::Match($content, $pattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
  if ($m.Success) { return $m.Groups[1].Value.Trim() }
  return ""
}

$version  = Get-FirstMatch 'Version:\s*</span>\s*<span>([^<]+)</span>'
$date     = Get-FirstMatch 'Date:\s*</span>\s*<span>([^<]+)</span>'
$duration = Get-FirstMatch 'Duration:\s*</span>\s*<span>([^<]+)</span>'
$desc     = Get-FirstMatch 'Description:\s*</span>\s*<span>([^<]+)</span>'

$rowMatch = [regex]::Match($content, '<tr id="ROOT"[^>]*>(.*?)</tr>', [System.Text.RegularExpressions.RegexOptions]::Singleline)
if (-not $rowMatch.Success) {
  Write-Error "Could not parse report stats (ROOT row missing)."
}

$values = @()
foreach ($m in [regex]::Matches($rowMatch.Groups[1].Value, '<td class="value [^"]*">([^<]+)</td>')) {
  $values += $m.Groups[1].Value.Trim()
}

if ($values.Count -lt 13) {
  Write-Error "Not enough stats fields to summarize."
}

$labels = @("total","ok","ko","koPct","reqPerSec","min","p50","p75","p95","p99","max","mean","stddev")
$stats = @{}
for ($i = 0; $i -lt $labels.Count; $i++) {
  $stats[$labels[$i]] = $values[$i]
}

$statusLine = if ([int]$stats["ko"] -gt 0) { "HAS FAILURES" } else { "ALL OK" }
$descLine = if ($desc -and $desc -ne "-" -and $desc -ne "&mdash;") { $desc } else { "none" }

$summary = @()
$summary += "Gatling Summary"
$summary += "Run: $runDir"
$summary += "Version: $version | Date: $date | Duration: $duration | Description: $descLine"
$summary += "Requests: total=$($stats["total"]) ok=$($stats["ok"]) ko=$($stats["ko"]) (KO%=$($stats["koPct"]))"
$summary += "Throughput: $($stats["reqPerSec"]) req/s"
$summary += "Latency(ms): min=$($stats["min"]) p50=$($stats["p50"]) p75=$($stats["p75"]) p95=$($stats["p95"]) p99=$($stats["p99"]) max=$($stats["max"]) mean=$($stats["mean"]) stddev=$($stats["stddev"])"
$summary += "Status: $statusLine"

$summaryText = ($summary -join "`r`n")
Write-Output $summaryText

$outPath = Join-Path $PSScriptRoot "target/summary.txt"
$summaryText | Out-File -Encoding utf8 -FilePath $outPath
Write-Output ""
Write-Output "Wrote: $outPath"
