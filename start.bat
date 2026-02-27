@echo off
chcp 65001 >nul
echo ========================================
echo   CocoMonyaB 启动脚本
echo ========================================
echo.

REM 设置 Java 编码为 UTF-8
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8

REM 查找并重命名 CocoMonyaB-*.jar 文件
for %%f in (CocoMonyaB-*.jar) do (
    if exist "%%f" (
        echo 找到文件: %%f
        echo 正在重命名为 CocoMonyaB.jar...
        move /Y "%%f" "CocoMonyaB.jar" >nul
        echo 重命名完成
        echo.
        goto :start_app
    )
)

:start_app
REM 检查 JAR 文件是否存在
if exist "CocoMonyaB.jar" (
    echo 正在启动应用...
    echo.
    java -Dfile.encoding=UTF-8 -DCONSOLE_CHARSET=UTF-8 -jar CocoMonyaB.jar
) else (
    echo [错误] 未找到 JAR 文件！
    echo 请先运行以下命令构建项目：
    echo   gradlew.bat build
    echo.
    pause
    exit /b 1
)

pause
