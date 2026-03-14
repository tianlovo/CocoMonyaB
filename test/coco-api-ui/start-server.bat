@echo off
chcp 65001 >nul
echo ==========================================
echo    Coco API UI Server 启动脚本
echo ==========================================
echo.

cd /d "%~dp0\server"

if not exist "node_modules" (
    echo 正在安装依赖...
    call pnpm install
    if errorlevel 1 (
        echo 依赖安装失败！
        pause
        exit /b 1
    )
)

echo.
echo 正在启动服务器...
echo.
pnpm dev

pause
