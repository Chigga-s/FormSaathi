param([switch]$DownloadModel)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$source = Join-Path $root 'app/src/main/cpp/whisper.cpp'
$revision = 'a8d002cfd879315632a579e73f0148d06959de36'
if (-not (Test-Path -LiteralPath $source)) {
    & git clone --depth 1 --branch v1.7.6 https://github.com/ggml-org/whisper.cpp.git $source
    if ($LASTEXITCODE -ne 0) { throw 'Whisper source download failed.' }
}
$actual = & git -C $source rev-parse HEAD
if ($LASTEXITCODE -ne 0 -or $actual -ne $revision) {
    throw "Whisper source must be at $revision. Existing checkout was left untouched."
}

if ($DownloadModel) {
    $expectedHash = '818710568DA3CA15689E31A743197B520007872FF9576237BDA97BD1B469C3D7'
    $directory = Join-Path $root 'app/src/main/assets/models'
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    $model = Join-Path $directory 'whisper-tiny-multilingual-q5.bin'
    if (-not (Test-Path -LiteralPath $model)) {
        $partial = "$model.partial"
        try {
            Invoke-WebRequest -UseBasicParsing -Uri 'https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny-q5_1.bin' -OutFile $partial
            if ((Get-Item -LiteralPath $partial).Length -lt 1000000) { throw 'Model download is incomplete.' }
            Move-Item -LiteralPath $partial -Destination $model
        } finally {
            if (Test-Path -LiteralPath $partial) { Remove-Item -LiteralPath $partial }
        }
    }
    $actualHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $model).Hash
    if ($actualHash -ne $expectedHash) {
        throw "Model SHA-256 mismatch. Expected $expectedHash but got $actualHash."
    }
}
