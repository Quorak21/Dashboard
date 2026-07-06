# Spike FMP + Finnhub — Dashboard multi-actifs
# Usage: copier backend/.env.example -> backend/.env, renseigner FMP_API_KEY et FINNHUB_API_KEY
#        pwsh scripts/spike-providers.ps1

$ErrorActionPreference = "Continue"
$envFile = Join-Path $PSScriptRoot "..\backend\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#=]+)=(.*)$') {
            $name = $matches[1].Trim()
            $val = $matches[2].Trim().Trim('"')
            if ($val) { Set-Item -Path "env:$name" -Value $val }
        }
    }
}

$fmpKey = $env:FMP_API_KEY
$fhKey = $env:FINNHUB_API_KEY

$assets = @(
    @{ Id = "inveb"; Name = "Investor AB"; FmpSymbol = "INVE-B.ST"; FhSymbol = "INVE-B.ST"; Isin = $null },
    @{ Id = "brwm"; Name = "BlackRock World Mining"; FmpSymbol = "BRWM.L"; FhSymbol = "BRWM.L"; Isin = $null },
    @{ Id = "o"; Name = "Realty Income"; FmpSymbol = "O"; FhSymbol = "O"; Isin = $null },
    @{ Id = "iii"; Name = "3i Group"; FmpSymbol = "III.L"; FhSymbol = "III.L"; Isin = $null },
    @{ Id = "chdiv"; Name = "UBS Swiss Dividend ETF"; FmpSymbol = "CHDIV.SW"; FhSymbol = "CHDIV.SW"; Isin = "CH1244681594" },
    @{ Id = "qqqe"; Name = "UBS Nasdaq 100 ETF"; FmpSymbol = "QQQE.SW"; FhSymbol = "QQQE.SW"; Isin = "IE0003RQ9F90" },
    @{ Id = "infr"; Name = "iShares Global Infra ETF"; FmpSymbol = "INFR.L"; FhSymbol = "INFR.L"; Isin = "IE00B1FZS467" }
)

function Test-FmpProfile($symbol) {
    if (-not $fmpKey) { return @{ ok = $false; note = "FMP_API_KEY manquante" } }
    try {
        $url = "https://financialmodelingprep.com/stable/profile?symbol=$symbol&apikey=$fmpKey"
        $r = Invoke-RestMethod -Uri $url -TimeoutSec 15
        if ($r -is [array] -and $r.Count -gt 0 -and $r[0].price) {
            return @{ ok = $true; price = $r[0].price; currency = $r[0].currency; mcap = $r[0].mktCap }
        }
        return @{ ok = $false; note = "réponse vide ou sans prix"; raw = ($r | ConvertTo-Json -Compress -Depth 3) }
    } catch {
        return @{ ok = $false; note = $_.Exception.Message }
    }
}

function Test-FhQuote($symbol) {
    if (-not $fhKey) { return @{ ok = $false; note = "FINNHUB_API_KEY manquante" } }
    try {
        $url = "https://finnhub.io/api/v1/quote?symbol=$symbol&token=$fhKey"
        $r = Invoke-RestMethod -Uri $url -TimeoutSec 15
        if ($r.c -and $r.c -gt 0) {
            return @{ ok = $true; price = $r.c; changePct = $r.dp }
        }
        return @{ ok = $false; note = "c=0 ou absent"; raw = ($r | ConvertTo-Json -Compress) }
    } catch {
        return @{ ok = $false; note = $_.Exception.Message }
    }
}

function Test-FhEtfProfile($isin, $symbol) {
    if (-not $fhKey) { return @{ ok = $false; note = "FINNHUB_API_KEY manquante" } }
    try {
        $url = "https://finnhub.io/api/v1/etf/profile?isin=$isin&token=$fhKey"
        $r = Invoke-RestMethod -Uri $url -TimeoutSec 15
        if ($r -and ($r.name -or $r.aum)) {
            return @{ ok = $true; name = $r.name; expenseRatio = $r.expenseRatio }
        }
        # repli symbole
        $url2 = "https://finnhub.io/api/v1/etf/profile?symbol=$symbol&token=$fhKey"
        $r2 = Invoke-RestMethod -Uri $url2 -TimeoutSec 15
        if ($r2 -and ($r2.name -or $r2.aum)) {
            return @{ ok = $true; name = $r2.name; via = "symbol" }
        }
        return @{ ok = $false; note = "profil ETF vide" }
    } catch {
        return @{ ok = $false; note = $_.Exception.Message }
    }
}

function Test-FhEtfSector($isin) {
    if (-not $fhKey) { return @{ ok = $false; note = "FINNHUB_API_KEY manquante" } }
    try {
        $url = "https://finnhub.io/api/v1/etf/sector-exposure?isin=$isin&token=$fhKey"
        $r = Invoke-RestMethod -Uri $url -TimeoutSec 15
        if ($r.sectorExposure -and $r.sectorExposure.Count -gt 0) {
            return @{ ok = $true; sectors = $r.sectorExposure.Count }
        }
        return @{ ok = $false; note = "pas de sectorExposure" }
    } catch {
        return @{ ok = $false; note = $_.Exception.Message }
    }
}

$results = @()
foreach ($a in $assets) {
    $row = [ordered]@{
        id = $a.Id
        name = $a.Name
        fmp_profile = (Test-FmpProfile $a.FmpSymbol)
        fh_quote = (Test-FhQuote $a.FhSymbol)
    }
    if ($a.Isin) {
        $row.fh_etf_profile = (Test-FhEtfProfile $a.Isin $a.FhSymbol)
        $row.fh_etf_sectors = (Test-FhEtfSector $a.Isin)
    }
    $results += [pscustomobject]$row
}

$outDir = Join-Path $PSScriptRoot "..\_bmad-output\planning-artifacts\research"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$outFile = Join-Path $outDir "spike-providers-results-$(Get-Date -Format 'yyyy-MM-dd').json"
$results | ConvertTo-Json -Depth 6 | Set-Content -Path $outFile -Encoding UTF8

Write-Host "`n=== SPIKE FMP + FINNHUB ===" -ForegroundColor Cyan
Write-Host "FMP key: $(if ($fmpKey) { 'OK' } else { 'MANQUANTE' })"
Write-Host "Finnhub key: $(if ($fhKey) { 'OK' } else { 'MANQUANTE' })"
Write-Host "Résultats: $outFile`n"

foreach ($r in $results) {
    $fmp = if ($r.fmp_profile.ok) { "FMP OK prix=$($r.fmp_profile.price)" } else { "FMP FAIL $($r.fmp_profile.note)" }
    $fh = if ($r.fh_quote.ok) { "FH quote OK=$($r.fh_quote.price)" } else { "FH quote FAIL $($r.fh_quote.note)" }
    Write-Host "$($r.id): $fmp | $fh"
}
