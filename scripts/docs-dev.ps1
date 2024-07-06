$oldErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "stop"

Set-Location $PSScriptRoot
Set-Location ..

# check exists venv
if (!(Test-Path -Path .venv)) {
  python3 -mvenv .venv
}

# activate venv
.venv\Scripts\activate
pip install -r requirements.txt

mkdocs serve --dev-addr=0.0.0.0:8080

$ErrorActionPreference = $oldErrorActionPreference