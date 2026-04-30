param(
    [Parameter(Mandatory = $true)]
    [string]$ProjectId,
    [string]$Region = "us-central1",
    [string]$ServiceName = "luxdrm-spring",
    [string]$DatasourceUrl,
    [string]$DatasourceUsername,
    [string]$DatasourcePassword,
    [string]$ServiceAccountKeyFile,
    [string]$FirebaseToken
)

$ErrorActionPreference = "Stop"

$gcloud = Join-Path $env:LOCALAPPDATA "Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd"
if (-not (Test-Path $gcloud)) {
    throw "gcloud not found. Install Google Cloud SDK first."
}

if (-not (Get-Command firebase -ErrorAction SilentlyContinue)) {
    throw "firebase CLI not found. Install with: npm i -g firebase-tools"
}

if (-not $ServiceAccountKeyFile -and $env:GOOGLE_APPLICATION_CREDENTIALS) {
    $ServiceAccountKeyFile = $env:GOOGLE_APPLICATION_CREDENTIALS
}

if (-not $FirebaseToken -and $env:FIREBASE_TOKEN) {
    $FirebaseToken = $env:FIREBASE_TOKEN
}

function Invoke-CheckedCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Executable,
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [Parameter(Mandatory = $true)]
        [string]$Step
    )

    Write-Host $Step
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Step failed with exit code $LASTEXITCODE."
    }
}

if ($ServiceAccountKeyFile) {
    if (-not (Test-Path $ServiceAccountKeyFile)) {
        throw "Service account key file not found: $ServiceAccountKeyFile"
    }

    Invoke-CheckedCommand `
        -Executable $gcloud `
        -Arguments @("auth", "activate-service-account", "--key-file", $ServiceAccountKeyFile) `
        -Step "Authenticating gcloud with service account key..."
}

Invoke-CheckedCommand `
    -Executable $gcloud `
    -Arguments @("services", "enable", "run.googleapis.com", "cloudbuild.googleapis.com", "artifactregistry.googleapis.com", "--project", $ProjectId) `
    -Step "Enabling required Google Cloud APIs..."

$image = "gcr.io/$ProjectId/$ServiceName"
Invoke-CheckedCommand `
    -Executable $gcloud `
    -Arguments @("builds", "submit", "--tag", $image, "--project", $ProjectId) `
    -Step "Building container image: $image"

$deployArgs = @(
    "run", "deploy", $ServiceName,
    "--image", $image,
    "--platform", "managed",
    "--region", $Region,
    "--allow-unauthenticated",
    "--project", $ProjectId
)

$envPairs = @()
if ($DatasourceUrl) { $envPairs += "SPRING_DATASOURCE_URL=$DatasourceUrl" }
if ($DatasourceUsername) { $envPairs += "SPRING_DATASOURCE_USERNAME=$DatasourceUsername" }
if ($DatasourcePassword) { $envPairs += "SPRING_DATASOURCE_PASSWORD=$DatasourcePassword" }
if ($envPairs.Count -gt 0) {
    $deployArgs += @("--set-env-vars", ($envPairs -join ","))
}

Invoke-CheckedCommand `
    -Executable $gcloud `
    -Arguments $deployArgs `
    -Step "Deploying Cloud Run service..."

$firebaseConfig = @"
{
  "hosting": {
    "public": "firebase-hosting-public",
    "ignore": [
      "firebase.json",
      "**/.*",
      "**/node_modules/**"
    ],
    "rewrites": [
      {
        "source": "**",
        "run": {
          "serviceId": "$ServiceName",
          "region": "$Region"
        }
      }
    ]
  }
}
"@

Set-Content -Path "firebase.json" -Value $firebaseConfig -Encoding UTF8

if (-not (Test-Path "firebase-hosting-public")) {
    New-Item -Path "firebase-hosting-public" -ItemType Directory | Out-Null
}

if (-not (Test-Path "firebase-hosting-public\index.html")) {
    Set-Content -Path "firebase-hosting-public\index.html" -Value "<!doctype html><title>LuxDRM</title><p>LuxDRM backend is served by Cloud Run.</p>" -Encoding UTF8
}

$firebaseDeployArgs = @("deploy", "--only", "hosting", "--project", $ProjectId)
if ($FirebaseToken) {
    $firebaseDeployArgs += @("--token", $FirebaseToken)
}

Invoke-CheckedCommand `
    -Executable "firebase" `
    -Arguments $firebaseDeployArgs `
    -Step "Deploying Firebase Hosting rewrite..."

$serviceUrl = & $gcloud run services describe $ServiceName --region $Region --project $ProjectId --format "value(status.url)"
if ($LASTEXITCODE -ne 0) {
    throw "Getting Cloud Run URL failed with exit code $LASTEXITCODE."
}
Write-Host ""
Write-Host "Cloud Run URL: $serviceUrl"
Write-Host "Firebase URL: https://$ProjectId.web.app"
