@echo off
echo Creating document symbolic links...

echo.
echo 1. Creating symbolic link for "api-response-spec.md"
mklink "api-response-spec.md" "E:\Develop\ToolDev\Application\CocoMonya\project\CocoMonyaB\docs\api\api-response-spec.md"
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