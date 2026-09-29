$ErrorActionPreference = 'Stop'
$libraryPath = Join-Path $PSScriptRoot 'libs'
New-Item -ItemType Directory -Force -Path $libraryPath | Out-Null
$dependencies = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'dependencies.json') -Raw | ConvertFrom-Json
foreach ($dependency in $dependencies) {
    if ([IO.Path]::GetFileName($dependency.file) -ne $dependency.file) { throw 'Invalid dependency filename' }
    $destination = Join-Path $libraryPath $dependency.file
    if (Test-Path -LiteralPath $destination) {
        if ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash -ine $dependency.sha256) {
            throw "Checksum mismatch in existing dependency: $destination"
        }
        Write-Host "Verified $($dependency.file)"
        continue
    }
    $download = "$destination.download"
    Invoke-WebRequest -Uri $dependency.url -OutFile $download
    if ((Get-FileHash -LiteralPath $download -Algorithm SHA256).Hash -ine $dependency.sha256) {
        throw "Download checksum mismatch: $download"
    }
    Move-Item -LiteralPath $download -Destination $destination
    Write-Host "Downloaded and verified $($dependency.file)"
}
