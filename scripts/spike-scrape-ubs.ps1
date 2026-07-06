# Spike scrape — CHDIV.SW + QQQE.SW (sources SIX)
# Usage: pwsh scripts/spike-scrape-ubs.ps1
# Sauvegarde fixtures HTML + rapport JSON dans _bmad-output/planning-artifacts/research/

$ErrorActionPreference = "Continue"

$outDir = Join-Path $PSScriptRoot "..\_bmad-output\planning-artifacts\research"
$fixtureDir = Join-Path $outDir "spike-scrape-fixtures"
New-Item -ItemType Directory -Force -Path $fixtureDir | Out-Null

$browserHeaders = @{
    "User-Agent"      = "DashboardBot/1.0 Spike (+https://dashboard.dokkcorp.ch)"
    "Accept"          = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    "Accept-Language" = "en-CH,en;q=0.9"
    "Accept-Encoding" = "identity"
}

$assets = @(
    @{
        id     = "chdiv"
        symbol = "CHDIV.SW"
        isin   = "CH1244681594"
        ubsUrl = "https://www.ubs.com/nl/en/assetmanagement/funds/etf/ch1244681594-ubs-etf-ch-msci-switzerland-imi-dividend-esg-chf-a-dis-pd001.html"
        yahoo  = "https://query1.finance.yahoo.com/v8/finance/chart/CHDIV.SW?interval=1d&range=1d"
    },
    @{
        id     = "qqqe"
        symbol = "QQQE.SW"
        isin   = "IE0003RQ9F90"
        ubsUrl = "https://www.ubs.com/nl/en/assetmanagement/funds/etf/ie0003rq9f90-ubs-nasdaq-100-ucits-etf-usd-dis-pd001.html"
        yahoo  = "https://query1.finance.yahoo.com/v8/finance/chart/QQQE.SW?interval=1d&range=1d"
        dbUrl  = "https://live.deutsche-boerse.com/etf/UBS-Nasdaq-100-UCITS-ETF-USD-dis?isin=IE0003RQ9F90"
    }
)

function Test-UbsPage($url, $fixtureName) {
    try {
        $r = Invoke-WebRequest -Uri $url -Headers $browserHeaders -TimeoutSec 45 -UseBasicParsing
        $path = Join-Path $fixtureDir "$fixtureName.html"
        $r.Content | Set-Content -Path $path -Encoding UTF8
        $hasFundgate = $r.Content -match 'fundgateHandler|georestrictedContent__placeholder'
        $hasBidAskSix = $r.Content -match 'SIX Swiss Exchange'
        $bid = $null; $ask = $null
        if ($r.Content -match 'SIX Swiss Exchange[^<]*(?:USD|CHF)[^|]*\|[^|]*\|[^|]*\|[^|]*\|\s*([0-9.]+)\s*\|\s*([0-9.]+)') {
            $bid = [decimal]$Matches[1]; $ask = [decimal]$Matches[2]
        }
        $nav = $null
        if ($r.Content -match 'Official NAV</[^>]+>[^|]*\|[^|]*(?:USD|CHF)\s+([0-9.]+)') {
            $nav = [decimal]$Matches[1]
        }
        return @{
            ok           = ($bid -ne $null -or $nav -ne $null)
            http         = $r.StatusCode
            length       = $r.Content.Length
            fundgateOnly = $hasFundgate -and -not $hasBidAskSix
            bid          = $bid
            ask          = $ask
            nav          = $nav
            note         = if ($bid) { "table SIX bid/ask" } elseif ($nav) { "NAV table" } elseif ($hasFundgate) { "fundgate shell sans prix" } else { "pas de prix detecte" }
        }
    } catch {
        $code = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { $null }
        return @{ ok = $false; http = $code; note = $_.Exception.Message }
    }
}

function Test-YahooChart($url) {
    try {
        $r = Invoke-RestMethod -Uri $url -Headers $browserHeaders -TimeoutSec 20
        $meta = $r.chart.result[0].meta
        return @{
            ok       = $true
            price    = $meta.regularMarketPrice
            currency = $meta.currency
            exchange = $meta.fullExchangeName
            symbol   = $meta.symbol
            note     = "Yahoo chart API (SIX via EBS)"
        }
    } catch {
        return @{ ok = $false; note = $_.Exception.Message }
    }
}

function Test-DeutscheBoerse($url, $fixtureName) {
    try {
        $r = Invoke-WebRequest -Uri $url -Headers $browserHeaders -TimeoutSec 45 -UseBasicParsing
        $path = Join-Path $fixtureDir "$fixtureName.html"
        $r.Content | Set-Content -Path $path -Encoding UTF8
        $price = $null
        if ($r.Content -match '"lastPrice"\s*:\s*([0-9.]+)') { $price = [decimal]$Matches[1] }
        return @{
            ok       = ($price -ne $null)
            price    = $price
            currency = "EUR"
            note     = "Frankfurt/Xetra listing (BCFN.DE), pas SIX USD"
            http     = $r.StatusCode
        }
    } catch {
        return @{ ok = $false; note = $_.Exception.Message }
    }
}

$results = @()
foreach ($a in $assets) {
    $row = [ordered]@{
        id     = $a.id
        symbol = $a.symbol
        isin   = $a.isin
        ubs_nl = (Test-UbsPage $a.ubsUrl "spike_$($a.id)_ubs_nl")
        yahoo  = (Test-YahooChart $a.yahoo)
    }
    if ($a.dbUrl) {
        $row.deutsche_boerse = (Test-DeutscheBoerse $a.dbUrl "spike_$($a.id)_db")
    }
    $results += [pscustomobject]$row
    Start-Sleep -Milliseconds 800
}

# Stabilité Yahoo (3 runs)
$stability = @()
foreach ($a in $assets) {
    $prices = @()
    1..3 | ForEach-Object {
        $y = Test-YahooChart $a.yahoo
        if ($y.ok) { $prices += $y.price }
        Start-Sleep -Seconds 1
    }
    $stability += [pscustomobject]@{
        id     = $a.id
        runs   = $prices -join ", "
        stable = ($prices | Select-Object -Unique).Count -le 1
    }
}

$date = Get-Date -Format "yyyy-MM-dd"
$jsonPath = Join-Path $outDir "spike-scrape-results-$date.json"
@{
    date      = $date
    assets    = $results
    stability = $stability
} | ConvertTo-Json -Depth 8 | Set-Content -Path $jsonPath -Encoding UTF8

Write-Host "`n=== SPIKE SCRAPE SIX ETF ===" -ForegroundColor Cyan
Write-Host "Fixtures: $fixtureDir"
Write-Host "JSON:     $jsonPath`n"

foreach ($r in $results) {
    Write-Host "$($r.id) ($($r.symbol))" -ForegroundColor Yellow
    $u = $r.ubs_nl
    Write-Host "  UBS NL  : $(if ($u.ok) { "OK bid=$($u.bid) ask=$($u.ask) nav=$($u.nav)" } else { "FAIL http=$($u.http) $($u.note)" })"
    $y = $r.yahoo
    Write-Host "  Yahoo   : $(if ($y.ok) { "OK $($y.price) $($y.currency) ($($y.exchange))" } else { "FAIL $($y.note)" })"
    if ($r.deutsche_boerse) {
        $d = $r.deutsche_boerse
        Write-Host "  DBoerse : $(if ($d.ok) { "OK $($d.price) $($d.currency) - $($d.note)" } else { "FAIL $($d.note)" })"
    }
}

Write-Host "`nStabilité Yahoo:" -ForegroundColor Cyan
$stability | ForEach-Object { Write-Host "  $($_.id): $($_.runs) stable=$($_.stable)" }
