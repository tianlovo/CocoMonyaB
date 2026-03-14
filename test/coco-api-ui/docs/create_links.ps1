Write-Host "Creating document symbolic links..." -ForegroundColor Green

Write-Host "`n1. Creating symbolic link for 'api-response-spec.md'" -ForegroundColor Cyan
try {
    New-Item -ItemType SymbolicLink -Path "api-response-spec.md" -Target "E:\Develop\ToolDev\Application\CocoMonya\project\CocoMonyaB\docs\api\api-response-spec.md"
    Write-Host "  Successfully created" -ForegroundColor Green
} catch {
    Write-Host "  Failed: $_" -ForegroundColor Red
    Write-Host "  Please run as Administrator or enable Developer Mode" -ForegroundColor Yellow
}

Write-Host "`n2. Creating symbolic link for 'api.md'" -ForegroundColor Cyan
try {
    New-Item -ItemType SymbolicLink -Path "api.md" -Target "E:\Develop\ToolDev\Application\CocoMonya\project\CocoMonyaB\docs\api\api.md"
    Write-Host "  Successfully created" -ForegroundColor Green
} catch {
    Write-Host "  Failed: $_" -ForegroundColor Red
    Write-Host "  Please run as Administrator or enable Developer Mode" -ForegroundColor Yellow
}

Write-Host "`nDone!" -ForegroundColor Green
Read-Host "Press Enter to continue..."