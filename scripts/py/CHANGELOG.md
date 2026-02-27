# 更新日志

## [未发布] - 2026-02-27

### 新增
- **版本发布工具 (`release_tool.py`)**
  - 自动构建 JAR 包（使用 Gradle bootJar）
  - 自动将 development 分支合并到 main 分支（Squash and merge 模式）
  - 从 build.gradle.kts 自动读取版本号
  - 自动创建和推送版本标签
  - 自动创建 GitHub Release 并上传 JAR 包
  - 支持多 Git 用户配置及选择
  - 完整的操作前检查和确认流程
  - 操作失败时自动回滚
  - 构建失败时立即退出程序
- 新增 `release_tool.bat` Windows 批处理启动脚本
- 新增 `RELEASE_GUIDE.md` 版本发布指南文档

### 改进
- 更新 README.md，添加版本发布工具的详细说明
- 更新 pyproject.toml，添加 release-tool 入口点和 requests 依赖

## [未发布] - 2026-02-20

### 新增
- 使用官方 `openai` Python 库替代 `requests` 直接调用
- 支持思维链模型（如 o1、o3-mini 等）
- **实时流式输出**：可以看到模型的生成过程
  - 显示思维链模型的思考过程（如果有）
  - 实时显示 JSON 生成内容
  - 显示 Token 使用统计
- 结构化 JSON 输出格式，包含以下字段：
  - `type`: 提交类型（必需）
  - `scope`: 作用域（可选）
  - `description`: 简短描述（必需）
  - `body`: 详细说明数组（可选，支持列表格式）
  - `footer`: 页脚信息（可选）
- JSON 格式自动验证机制
- 智能重试机制：
  - API 调用失败重试（最多 3 次，可通过 `MAX_RETRIES` 配置）
  - Git 推送失败重试（最多 3 次，可通过 `PUSH_RETRIES` 配置）
- **检测所有远程仓库的未推送状态**
  - 显示每个远程仓库的未推送提交数
  - 以表格形式展示状态
  - 自动推送到所有有未推送提交的远程仓库
- 新增 `validate_commit_json()` 函数验证提交信息格式
- 新增 `format_commit_message()` 函数将 JSON 转换为标准提交信息
- 新增 `get_unpushed_status()` 函数获取所有远程仓库状态

### 改进
- 使用 `response_format={"type": "json_object"}` 强制 JSON 输出
- **启用流式输出 (`stream=True`)**，实时显示生成过程
- **body 字段支持数组格式**，自动转换为列表形式（`- item1\n- item2`）
- **Git 推送重试机制**，自动识别网络错误并重试
- **检查所有远程分支**，不仅仅是 origin
- 更详细的错误提示和重试信息
- 更严格的提交类型验证（支持 11 种标准类型）
- 彩色输出区分思考过程和生成结果
- 显示 Token 使用统计信息

### 依赖变更
- 移除: `requests>=2.31.0`
- 新增: `openai>=1.0.0`

### 修复
- **修复 Windows 系统编码问题**
  - 在 Windows 上使用 GBK 编码处理 Git 命令输出
  - 使用 `errors="replace"` 处理无法解码的字符
  - 修复 `UnicodeDecodeError: 'gbk' codec can't decode byte` 错误
- **修复 git commit 函数的 NoneType 错误**
  - 正确处理 `stdout` 和 `stderr` 可能为 None 的情况
  - 添加异常处理确保错误信息正确显示
- **修复 Git 推送 SSL/TLS 连接失败问题**
  - 添加自动重试机制
  - 识别网络相关错误并重试
  - 非网络错误直接失败，避免无效重试
- **修复在子目录运行时无法提交的问题**
  - 新增 `get_git_root()` 函数获取 Git 仓库根目录
  - 所有 Git 操作（add、commit、push）现在都在根目录执行
  - 解决了在 `scripts/py/` 目录运行时 `git add .` 只添加当前目录文件的问题
- 修复思维链模型输出被截断的问题
  - 使用 `max_completion_tokens` 替代 `max_tokens`（思维链的 reasoning 不计入此限制）
  - 分离 reasoning 和 content 的处理逻辑
  - reasoning 只显示不收集，content 显示并收集用于解析
- 添加更详细的错误信息（显示内容长度和预览）
- 添加空内容检查

### 配置变更
- 新增环境变量 `MAX_RETRIES`（默认值：3）- API 调用重试次数
- 新增环境变量 `MAX_COMPLETION_TOKENS`（默认值：2000）- 模型输出限制
- 新增环境变量 `PUSH_RETRIES`（默认值：3）- Git 推送重试次数
- 新增环境变量 `RETRY_DELAY`（默认值：2）- 重试延迟（秒）

## JSON 输出格式示例

```json
{
  "type": "feat",
  "scope": "commit-tool",
  "description": "支持思维链模型和结构化输出",
  "body": [
    "使用 openai 库替代 requests",
    "添加 JSON 格式验证",
    "实现自动重试机制",
    "支持列表格式的提交信息"
  ],
  "footer": null
}
```

生成的提交信息：
```
feat(commit-tool): 支持思维链模型和结构化输出

- 使用 openai 库替代 requests
- 添加 JSON 格式验证
- 实现自动重试机制
- 支持列表格式的提交信息
```

## 迁移指南

如果你已经在使用旧版本的 `commit_tool.py`，请按以下步骤升级：

1. 更新依赖：
   ```bash
   uv sync
   ```

2. 更新 `.env` 文件（可选）：
   ```env
   # 添加重试配置
   MAX_RETRIES=3
   MAX_COMPLETION_TOKENS=2000
   
   # 添加推送重试配置
   PUSH_RETRIES=3
   RETRY_DELAY=2
   ```

3. 测试新版本：
   ```bash
   uv run commit-tool
   ```

## 兼容性说明

- 完全兼容所有 OpenAI API 兼容服务
- 支持思维链模型的推理过程
- 向后兼容旧的环境变量配置
- **跨平台支持**：
  - Windows：自动使用 GBK 编码处理 Git 输出
  - Linux/macOS：使用 UTF-8 编码
  - 自动处理编码错误，确保程序稳定运行
- **多远程仓库支持**：
  - 自动检测所有配置的远程仓库
  - 显示每个远程仓库的推送状态
  - 支持同时推送到多个远程仓库
