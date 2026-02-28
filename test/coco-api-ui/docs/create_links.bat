@echo off
echo Creating document symbolic links...

echo.
echo 1. Creating symbolic link for "API Response Specification Document.md"
mklink "API Response Specification Document.md" "E:\Develop\ToolDev\Application\CocoMonya\project\CocoMonyaB\docs\api\API 响应规范文档.md"
if errorlevel 1 (
    echo Failed to create. Please run as Administrator.
)

echo.
echo 2. Creating symbolic link for "api.md"
mklink "api.md" "E:\Develop\ToolDev\Application\CocoMonya\project\CocoMonyaB\docs\api\api.md"
if errorlevel 1 (
    echo Failed to create. Please run as Administrator.
)

echo.
echo Done!
pause