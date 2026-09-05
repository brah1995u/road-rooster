param(
    [Parameter(Mandatory = $true)][string]$Apk,
    [Parameter(Mandatory = $true)][string]$Zip,
    [long]$MaxBytes = 10000000
)

# Input must already have passed signing, alignment and install/run verification.
$ErrorActionPreference = 'Stop'
$apkFile = Get-Item -LiteralPath $Apk
if ($apkFile.Extension -ne '.apk' -or $apkFile.PSIsContainer) { throw 'Expected one APK file' }
if ($apkFile.Length -ge $MaxBytes) { throw "APK exceeds strict byte cap: $($apkFile.Length) >= $MaxBytes" }
if (Test-Path -LiteralPath $Zip) { throw 'Destination already exists; keep previous artifacts intact' }

Compress-Archive -LiteralPath $apkFile.FullName -DestinationPath $Zip -CompressionLevel Optimal
$zipFile = Get-Item -LiteralPath $Zip
if ($zipFile.Length -ge $MaxBytes) { throw "ZIP exceeds strict byte cap: $($zipFile.Length) >= $MaxBytes" }

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($zipFile.FullName)
try {
    if ($archive.Entries.Count -ne 1 -or $archive.Entries[0].FullName -ne $apkFile.Name) {
        throw 'ZIP must contain exactly the expected APK at the root'
    }
    $entryStream = $archive.Entries[0].Open()
    $hasher = [Security.Cryptography.SHA256]::Create()
    try {
        $embeddedHash = [BitConverter]::ToString($hasher.ComputeHash($entryStream)).Replace('-', '')
    } finally {
        $entryStream.Dispose()
        $hasher.Dispose()
    }
    $sourceHash = (Get-FileHash -LiteralPath $apkFile.FullName -Algorithm SHA256).Hash
    if ($sourceHash -ne $embeddedHash) { throw 'Embedded APK integrity mismatch' }
    [pscustomobject]@{
        APKBytes = $apkFile.Length
        ZIPBytes = $zipFile.Length
        LimitBytes = $MaxBytes
        Entry = $apkFile.Name
        APKSHA256 = $sourceHash
        ZIPSHA256 = (Get-FileHash -LiteralPath $zipFile.FullName -Algorithm SHA256).Hash
        Integrity = 'PASS'
    } | ConvertTo-Json
} finally {
    $archive.Dispose()
}
