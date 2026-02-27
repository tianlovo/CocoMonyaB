# CocoMonyaB Python 工具脚本

此目录包含 CocoMonyaB 项目的 Python 工具脚本集合，用于自动化开发工作流和项目维护任务。

## 目录结构

```
scripts/py/
├── .env.example           # 环境变量配置模板
├── .env                  # 环境变量配置（本地，不提交）
├── pyproject.toml        # Python 项目配置和依赖
├── uv.lock              # 依赖锁文件
├── README.md            # 本文档
└── *.py                 # 各个工具脚本文件
```

## 可用工具

### 1. 一键提交工具 (`commit_tool.py`)

基于大模型 API 的智能 Git 提交工具，自动分析代码改动并生成符合 Conventional Commits 规范的提交信息。

**功能特性：**
- 📊 自动分析 git 状态和 diff 信息
- 🤖 调用大模型 API 生成规范的提交信息（中文）
- 🔧 支持 Conventional Commits 规范
- 👥 支持多 Git 用户配置及选择
- 🔐 使用 GitHub Token 进行认证推送
- 🌐 自动推送到 GitHub 远程仓库
- 🔄 智能重试机制（API 调用和 Git 推送）
- 📝 结构化列表输出（body 使用数组格式）
- ⚙️ 配置文件驱动，易于定制
- 🧠 支持思维链模型（如 o1、o3-mini 等）
- 🛡️ 网络错误自动重试，提高推送成功率

**快速开始：**

1. **配置环境变量**
   ```bash
   cp .env.example .env
   ```
   编辑 `.env` 文件，设置必要的变量（参见 `.env.example`）

2. **运行工具**
   ```bash
   # Windows
   commit_tool.bat

   # 或直接运行 Python 脚本
   python commit_tool.py
   ```

**工作流程：**
1. 选择 Git 用户（如果配置了多个用户）
2. 检查当前 git 仓库状态
3. 分析所有暂存和非暂存的更改
4. 如果没有更改，检查是否有未推送的提交
5. 调用大模型 API 生成提交信息
6. 确认并提交更改
7. 推送到 GitHub 远程仓库

---

### 2. 版本发布工具 (`release_tool.py`)

将 development 分支使用 Squash and merge 模式合并到 main 分支，自动构建 JAR 包，并创建 GitHub Release 上传版本包。

**功能特性：**
- 🏗️ 自动构建 JAR 包（使用 Gradle bootJar）
- 🔍 自动检查工作区状态（确保干净）
- 🔄 验证 development 分支是否为最新
- 🔀 使用 Squash and merge 模式合并分支
- 📦 从 build.gradle.kts 自动读取版本号
- 🏷️ 自动创建和推送版本标签
- 🚀 自动创建 GitHub Release 并上传 JAR 包
- 👥 支持多 Git 用户配置及选择
- 🔐 使用 GitHub Token 进行认证推送
- ✅ 完整的操作前确认流程
- 🛡️ 操作失败时自动回滚
- ⚠️ 构建失败时立即退出程序

**快速开始：**

1. **配置环境变量**
   
   确保 `.env` 文件已配置（与 commit_tool 共享配置）

2. **运行工具**
   ```bash
   # Windows
   release_tool.bat

   # 或直接运行 Python 脚本
   python release_tool.py
   ```

**工作流程：**
1. 检查 Git 仓库状态
2. 构建 JAR 包（失败则退出）
3. 选择 Git 用户（如果配置了多个用户）
4. 获取远程仓库最新信息
5. 检查工作区是否干净
6. 验证 development 分支是否存在且为最新
7. 从 build.gradle.kts 读取版本号
8. 确认发布操作
9. 切换到 main 分支
10. 使用 Squash and merge 模式合并 development 分支
11. 推送 main 分支到远程仓库
12. 创建版本标签（格式：v{version}）
13. 推送标签到远程仓库
14. 创建 GitHub Release 并上传 JAR 包
15. 切换回 development 分支

**使用前提：**
- 本地所有分支工作区必须干净（无未提交的更改）
- development 分支必须存在且与远程同步
- build.gradle.kts 文件中必须包含有效的版本号定义
- 项目能够成功构建（./gradlew clean bootJar）
- GitHub Token 必须具有创建 Release 和上传文件的权限

**注意事项：**
- development 分支会被保留，不会被删除
- 如果标签已存在，会提示是否删除并重新创建
- 操作失败时会自动切换回 development 分支
- 构建失败会在程序开头立即退出，不会执行后续操作
- JAR 包会自动上传到 GitHub Release 的 Assets 中

## 添加新工具

要添加新的 Python 工具脚本：

1. 在 `scripts/py/` 目录下创建新的 `.py` 文件
2. 如有需要，在 `pyproject.toml` 的 `[project.scripts]` 部分添加新的入口点
3. 如有新依赖，添加到 `pyproject.toml` 的 `dependencies` 部分
4. 运行 `uv sync` 更新依赖
5. 更新此 README 文档，添加新工具的介绍

## 命名规范

- 工具脚本文件以 `_tool.py` 结尾（如 `commit_tool.py`）
- 命令行入口点在 `pyproject.toml` 中以连字符命名（如 `commit-tool`）

## 依赖管理

使用 [uv](https://github.com/astral-sh/uv) 进行 Python 依赖管理：

```bash
# 安装依赖
uv sync

# 添加新依赖
uv add package-name

# 更新依赖
uv sync --upgrade
```

## 注意事项

- `.env` 文件已添加到项目根目录的 `.gitignore`，不会被提交到版本库
- 所有工具脚本共享同一套依赖和环境配置
- 建议使用虚拟环境（uv 会自动创建 `.venv/`）

## 故障排除

### Windows 编码问题

如果在 Windows 上遇到 `UnicodeDecodeError: 'gbk' codec can't decode byte` 错误：

1. 确保使用最新版本的工具（已修复此问题）
2. 运行测试脚本验证编码处理：
   ```bash
   python test_encoding.py
   ```

### 思维链模型输出被截断

如果使用思维链模型（如 deepseek-reasoner、o1、o3-mini）时输出被截断：

1. 增加 `MAX_COMPLETION_TOKENS` 配置：
   ```env
   MAX_COMPLETION_TOKENS=4000
   ```
2. 注意：思维链模型的 reasoning 过程不计入此限制

### API 调用失败

如果 API 调用失败：

1. 检查 `.env` 文件中的 `API_KEY` 是否正确
2. 检查 `API_BASE_URL` 是否正确
3. 检查网络连接
4. 查看错误信息中的详细提示

### Git 推送失败（SSL/TLS 错误）

如果推送时遇到 `schannel: failed to receive handshake, SSL/TLS connection failed` 错误：

1. 工具会自动重试（默认 3 次）
2. 可以调整重试配置：
   ```env
   PUSH_RETRIES=5        # 增加重试次数
   RETRY_DELAY=3         # 增加重试延迟（秒）
   ```
3. 检查网络连接和防火墙设置
4. 尝试使用 SSH 方式推送（修改远程仓库 URL）
5. 如果某个远程仓库持续失败，可以临时从 `.env` 中移除该配置