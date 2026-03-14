# Coco API UI Server 启动脚本
$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "   Coco API UI Server 启动脚本" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

$serverPath = Join-Path $PSScriptRoot "server"
Set-Location $serverPath

# 检查是否需要安装依赖
if (-not (Test-Path "node_modules")) {
    Write-Host "正在安装依赖..." -ForegroundColor Yellow
    try {
        pnpm install
        Write-Host "依赖安装完成！" -ForegroundColor Green
    } catch {
        Write-Host "依赖安装失败！" -ForegroundColor Red
        Read-Host "按回车键退出"
        exit 1
    }
}

Write-Host ""
Write-Host "正在启动服务器..." -ForegroundColor Green
Write-Host ""

pnpm dev

Read-Host "按回车键退出"
