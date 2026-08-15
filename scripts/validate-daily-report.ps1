param(
    [Parameter(Mandatory = $true)]
    [string]$Path
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
    Write-Error "Daily report file not found."
    exit 1
}

$requiredHeadings = @(
    '# Royal Shield Daily Report',
    '## Objetivo del día',
    '## Estado inicial',
    '## Línea de tiempo',
    '## Archivos modificados',
    '## Validaciones realizadas',
    '## Estado de funciones',
    '## Riesgos o bloqueadores',
    '## Próximo paso recomendado'
)

$lines = Get-Content -LiteralPath $Path
$text = $lines -join "`n"
$errors = [System.Collections.Generic.List[string]]::new()

foreach ($heading in $requiredHeadings) {
    if (-not $text.Contains($heading)) {
        $errors.Add("missing-heading: $heading")
    }
}

if (-not ($lines | Where-Object { $_ -match '^### (?:[01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9] (?:EDT|EST)$' })) {
    $errors.Add('missing-valid-timeline-timestamp')
}

$sensitiveRules = [ordered]@{
    'bearer-token' = '(?i)Bearer\s+[A-Za-z0-9._~+/-]{12,}'
    'jwt-shape' = '\beyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\b'
    'api-key-assignment' = '(?i)(?:api[_-]?key|auth[_-]?token|account[_-]?sid|secret)\s*[:=]\s*["'']?[A-Za-z0-9_./+~-]{8,}'
    'e164-phone' = '(?<![\w.-])\+[1-9]\d{7,14}(?!\d)'
    'coordinate-pair' = '(?<!\d)-?(?:[0-8]?\d(?:\.\d{4,})?|90(?:\.0+)?)\s*[,/]\s*-?(?:1[0-7]\d(?:\.\d{4,})?|\d?\d(?:\.\d{4,})?|180(?:\.0+)?)(?!\d)'
}

for ($index = 0; $index -lt $lines.Count; $index++) {
    foreach ($rule in $sensitiveRules.GetEnumerator()) {
        if ($lines[$index] -match $rule.Value) {
            $errors.Add("sensitive-pattern:$($rule.Key):line:$($index + 1)")
        }
    }
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output "Daily report validation passed: $Path"
exit 0
