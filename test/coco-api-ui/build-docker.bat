@echo off
chcp 65001 >nul
echo ==========================================
echo    Coco API UI Docker构建准备脚本
echo ==========================================
echo.

set "SOURCE_DIR=%~dp0"
set "BUILD_DIR=%SOURCE_DIR%build\docker"

echo [1/5] 清理旧的构建目录...
if exist "%BUILD_DIR%" (
    rmdir /s /q "%BUILD_DIR%"
    echo 已清理旧构建目录
)

echo.
echo [2/5] 创建构建目录...
mkdir "%BUILD_DIR%"
echo 构建目录: %BUILD_DIR%

echo.
echo [3/5] 构建Vue前端...
cd /d "%SOURCE_DIR%"
call pnpm install
if errorlevel 1 (
    echo 前端依赖安装失败！
    pause
    exit /b 1
)

call pnpm build
if errorlevel 1 (
    echo 前端构建失败！
    pause
    exit /b 1
)
echo Vue前端构建完成

echo.
echo [4/5] 构建Node后端...
cd /d "%SOURCE_DIR%\server"
call pnpm install
if errorlevel 1 (
    echo 后端依赖安装失败！
    pause
    exit /b 1
)

call pnpm build
if errorlevel 1 (
    echo 后端构建失败！
    pause
    exit /b 1
)
echo Node后端构建完成

echo.
echo [5/5] 复制构建产物到build/docker目录...

:: 创建目录结构
mkdir "%BUILD_DIR%\server"
mkdir "%BUILD_DIR%\docker"

:: 复制Vue前端构建产物（Docker直接使用，不需要源码）
xcopy /s /e /i /y "%SOURCE_DIR%\dist" "%BUILD_DIR%\dist" >nul
echo - Vue前端构建产物已复制

:: 复制Node后端构建产物
xcopy /s /e /i /y "%SOURCE_DIR%\server\dist" "%BUILD_DIR%\server\dist\" >nul
copy /y "%SOURCE_DIR%\server\package.json" "%BUILD_DIR%\server\" >nul
if exist "%SOURCE_DIR%\server\pnpm-lock.yaml" (
    copy /y "%SOURCE_DIR%\server\pnpm-lock.yaml" "%BUILD_DIR%\server\" >nul
)
echo - Node后端构建产物已复制

:: 复制Dockerfile
copy /y "%SOURCE_DIR%\Dockerfile" "%BUILD_DIR%\" >nul
echo - Dockerfile已复制

:: 复制docker-compose.yaml
copy /y "%SOURCE_DIR%\docker-compose.yaml" "%BUILD_DIR%\" >nul
echo - docker-compose.yaml已复制

:: 复制docker配置目录（包含config.yaml）
xcopy /s /e /i /y "%SOURCE_DIR%\docker" "%BUILD_DIR%\docker\" >nul
echo - docker配置目录已复制（包含config.yaml）

echo.
echo [6/6] 创建构建说明文件...
(
echo # Coco API UI Docker构建说明
echo.
echo ## 构建产物说明
echo.
echo 本目录包含构建后的产物和Docker配置文件，可以直接构建Docker镜像。
echo Vue前端和Node后端已在本地构建完成，Dockerfile直接使用构建产物。
echo.
echo ## 目录结构
echo.
echo ```
echo build/docker/
echo ├── dist/                  # Vue前端构建产物（预构建）
echo ├── server/                # Node后端构建产物
echo │   ├── dist/              # 编译后的JS文件
echo │   └── package.json       # 后端依赖配置
echo ├── docker/                # Docker配置
echo │   ├── nginx.conf         # nginx配置文件
 echo │   ├── start.sh           # 容器启动脚本
 echo │   └── config.yaml        # 配置文件
 echo ├── Dockerfile             # Docker镜像构建文件
echo ├── docker-compose.yaml    # Docker Compose配置
echo └── README.md              # 本说明文件
echo ```
echo.
echo ## 构建Docker镜像
echo.
echo 在当前目录执行：
echo.
echo ```bash
echo # 构建镜像
echo docker build -t coco-api-ui:latest .
echo.
echo # 或使用docker-compose
echo docker-compose up -d
echo ```
echo.
echo ## 端口说明
echo.
echo - 80: Vue前端（nginx）
echo - 15088: Node后端（固定端口，不可修改）
echo.
echo ## 配置说明
echo.
echo 配置文件位于 `docker/config.yaml`，构建时会自动复制到构建目录。
echo.
echo ### 使用前必须修改的配置
echo.
echo 编辑 `docker/config.yaml`：
echo.
echo 1. **Java后端地址**（必须修改）
echo    - Docker中运行时，不能直接使用127.0.0.1
echo    - Windows/Mac: `http://host.docker.internal:10721`
echo    - Linux: `http://172.17.0.1:10721` 或宿主机IP
echo.
echo 2. **前端登录令牌**（建议修改）
echo    - 用于前端页面登录认证
echo    - 建议修改为强密码
echo.
echo 3. **Bark通知**（可选）
echo    - 用于接收Java后端掉线、TG登录态失效等告警
echo.
echo ### 挂载配置
echo.
echo docker-compose.yaml中已默认挂载配置：
echo.
echo ```yaml
echo volumes:
echo   - ./docker/config.yaml:/app/server/config.yaml
echo ```
) > "%BUILD_DIR%\README.md"

echo 构建说明文件已创建

echo.
echo ==========================================
echo    构建准备完成！
echo ==========================================
echo.
echo 构建产物目录: %BUILD_DIR%
echo.
echo 请进入该目录执行以下命令构建Docker镜像：
echo.
echo   cd build\docker
echo   docker build -t coco-api-ui:latest .
echo.
echo 或使用 docker-compose：
echo.
echo   cd build\docker
echo   docker-compose up -d
echo.
echo ==========================================

pause
