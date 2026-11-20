$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path

Push-Location (Join-Path $repoRoot 'backend')
try {
    mvn test
    if ($LASTEXITCODE -ne 0) { throw "Backend tests failed with exit code $LASTEXITCODE" }
}
finally {
    Pop-Location
}

Push-Location (Join-Path $repoRoot 'frontend\admin')
try {
    npm run build
    if ($LASTEXITCODE -ne 0) { throw "Admin build failed with exit code $LASTEXITCODE" }
}
finally {
    Pop-Location
}

Push-Location (Join-Path $repoRoot 'frontend\miniprogram')
try {
    npm run typecheck
    if ($LASTEXITCODE -ne 0) { throw "Mini Program typecheck failed with exit code $LASTEXITCODE" }
}
finally {
    Pop-Location
}

