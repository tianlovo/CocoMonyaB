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
- 🔧 支持 Conventional Commits 规范 (https://www.conventionalcommits.org/zh-hans/v1.0.0/)
- 🌐 自动推送到所有配置的远程仓库
- 🔍 检测所有远程仓库的未推送状态
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
   编辑 `.env` 文件，设置以下变量：
   ```env
   # 大模型 API 配置
   API_BASE_URL=https://api.openai.com/v1
   API_KEY=your_api_key_here
   MODEL_ID=gpt-4o-mini

   # API 重试配置
   MAX_RETRIES=3
   MAX_COMPLETION_TOKENS=2000

   # Git 推送重试配置
   PUSH_RETRIES=3
   RETRY_DELAY=2

   # 远程仓库配置（可选）
   REMOTE_ORIGIN=https://github.com/tianlovo/CocoMonyaB.git
   REMOTE_CODEBERG=https://codeberg.org/tianluo/CocoMonyaB.git

   # Git 用户配置（可选）
   GIT_USER_NAME=your_name
   GIT_USER_EMAIL=your_email@example.com
   ```

2. **安装依赖**
   ```bash
   uv sync
   ```

3. **运行工具**
   ```bash
   # 使用 uv 运行（推荐）
   uv run commit-tool

   # 或直接运行 Python 脚本
   python scripts/py/commit_tool.py
   ```

**工作流程：**
1. 检查当前 git 仓库状态
2. 分析所有暂存和非暂存的更改
3. 如果没有更改，检查所有远程仓库的未推送状态
4. 调用大模型 API 生成提交信息（列表格式）
5. 确认并提交更改
6. 推送到所有远程仓库（带重试机制）

**支持的 API 服务：**
- OpenAI（包括 o1、o3-mini 等思维链模型）
- Azure OpenAI
- DeepSeek
- 其他兼容 OpenAI API 的服务

**技术亮点：**
- 使用官方 `openai` Python 库，支持最新特性
- **实时流式输出**，可视化模型生成过程
- 彩色区分思考过程（黄色）和生成结果（绿色）
- 显示 Token 使用统计，便于成本控制
- 强制 JSON 输出模式（`response_format={"type": "json_object"}`）
- 自动验证生成的提交信息格式
- 失败自动重试，最多重试 3 次（可配置）
- 支持思维链模型的推理过程

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