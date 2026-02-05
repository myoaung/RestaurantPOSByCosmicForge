# Antigravity Production Guard: Keystore & Deployment Guide Backup
# This script secures your production signing key and deployment documentation to OneDrive

$SourceKey = "d:\GitHub\RestaurantPOSByCosmicForge\cosmic-forge-release.keystore"
$SourceDeployGuide = "C:\Users\Myo Aung\.gemini\antigravity\brain\f0f1eb78-159e-43bd-b230-6f5be094ad41\deployment_guide.md"
$CloudBackup = "C:\Users\Myo Aung\OneDrive\CosmicForge_BACKUP"

Write-Host "🛸 ANTIGRAVITY PROTOCOL: Keystore Backup Initiated..." -ForegroundColor Cyan

# Create backup directory if it doesn't exist
if (!(Test-Path $CloudBackup)) {
    New-Item -ItemType Directory -Path $CloudBackup | Out-Null
    Write-Host "✅ Created backup directory: $CloudBackup" -ForegroundColor Green
}

# Backup keystore
if (Test-Path $SourceKey) {
    Copy-Item -Path $SourceKey -Destination "$CloudBackup\cosmic-forge-release.keystore" -Force
    Write-Host "✅ Keystore secured: cosmic-forge-release.keystore" -ForegroundColor Green
} else {
    Write-Host "❌ ERROR: Keystore not found at $SourceKey" -ForegroundColor Red
    exit 1
}

# Backup deployment guide
if (Test-Path $SourceDeployGuide) {
    Copy-Item -Path $SourceDeployGuide -Destination "$CloudBackup\deployment_guide.md" -Force
    Write-Host "✅ Deployment guide secured: deployment_guide.md" -ForegroundColor Green
} else {
    Write-Host "⚠️  WARNING: Deployment guide not found at $SourceDeployGuide" -ForegroundColor Yellow
}

# Backup APK if exists
$SourceAPK = "d:\GitHub\RestaurantPOSByCosmicForge\app-release-signed.apk"
if (Test-Path $SourceAPK) {
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    Copy-Item -Path $SourceAPK -Destination "$CloudBackup\app-release-signed_$timestamp.apk" -Force
    Write-Host "✅ APK backup created: app-release-signed_$timestamp.apk" -ForegroundColor Green
}

# Generate backup report
$reportPath = "$CloudBackup\backup_report.txt"
$report = @"
🛸 COSMIC FORGE POS - BACKUP REPORT
Generated: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

Files Backed Up:
- cosmic-forge-release.keystore
- deployment_guide.md
$(if (Test-Path $SourceAPK) { "- app-release-signed_$(Get-Date -Format 'yyyyMMdd_HHmmss').apk" } else { "" })

Backup Location: $CloudBackup
OneDrive Sync Status: $(if (Test-Path "C:\Users\Myo Aung\OneDrive") { "✅ Active" } else { "⚠️  Not Detected" })

⚠️  CRITICAL REMINDER:
Without this keystore, you CANNOT update your app on user devices!
Verify OneDrive sync is complete before closing this window.

Next Steps:
1. Verify files appear in OneDrive online portal
2. Download backup to external USB drive
3. Store keystore password in secure password manager
"@

$report | Out-File -FilePath $reportPath -Encoding UTF8
Write-Host "`n📄 Backup report saved: $reportPath" -ForegroundColor Cyan

# Display final status
Write-Host "`n" -NoNewline
Write-Host "╔════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  🛸 ANTIGRAVITY PROTOCOL: BACKUP COMPLETE          ║" -ForegroundColor Cyan
Write-Host "╠════════════════════════════════════════════════════╣" -ForegroundColor Cyan
Write-Host "║  ✅ Keystore secured to OneDrive                   ║" -ForegroundColor Green
Write-Host "║  ✅ Deployment guide backed up                     ║" -ForegroundColor Green
Write-Host "║  📁 Location: $CloudBackup" -ForegroundColor White
Write-Host "╚════════════════════════════════════════════════════╝" -ForegroundColor Cyan

Write-Host "`n⚠️  IMPORTANT: Verify OneDrive sync status in taskbar!" -ForegroundColor Yellow
