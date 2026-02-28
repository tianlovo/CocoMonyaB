#!/usr/bin/env python3
"""
一键分析当前所有改动并生成规范提交信息并推送至 GitHub 远程仓库
支持多 Git 用户配置及 Token 认证推送
"""

import os

# 设置 UTF-8 编码
os.environ["PYTHONUTF8"] = "1"
os.environ["PYTHONIOENCODING"] = "utf-8"
import sys
import subprocess
import json
import time
import re
from pathlib import Path
from typing import List, Tuple, Optional, Dict
from dotenv import load_dotenv
from openai import OpenAI
from rich.console import Console
from rich.panel import Panel
from rich.table import Table
from rich.progress import Progress, SpinnerColumn, TextColumn
from rich.syntax import Syntax
from rich.prompt import Prompt, Confirm
from rich.layout import Layout
from rich.live import Live
from rich.text import Text
from rich import print as rprint
from urllib.parse import urlparse, urlunparse

# 加载环境变量
env_path = Path(__file__).parent / ".env"
load_dotenv(dotenv_path=env_path)

# 配置
API_BASE_URL = os.getenv("API_BASE_URL", "https://api.openai.com/v1")
API_KEY = os.getenv("API_KEY", "")
MODEL_ID = os.getenv("MODEL_ID", "gpt-4o-mini")
MAX_RETRIES = int(os.getenv("MAX_RETRIES", "3"))
MAX_COMPLETION_TOKENS = int(os.getenv("MAX_COMPLETION_TOKENS", "2000"))

# 远程仓库配置 (仅支持 GitHub)
REMOTE_ORIGIN = os.getenv("REMOTE_ORIGIN", "")

# 解析多用户配置
# 格式示例: GIT_USERS=[{"name": "User1", "email": "email1@example.com", "token": "ghp_xxx"}, ...]
RAW_GIT_USERS = os.getenv("GIT_USERS", "[]")
try:
    GIT_USERS: List[Dict[str, str]] = json.loads(RAW_GIT_USERS)
    if not isinstance(GIT_USERS, list):
        raise ValueError("GIT_USERS must be a list")
except json.JSONDecodeError:
    console = Console()
    console.print("[bold red]错误: GIT_USERS 环境变量格式错误，请检查是否为合法的 JSON 数组[/bold red]")
    sys.exit(1)

# Rich 控制台
console = Console(color_system="auto")


def get_git_root() -> str:
    """获取 Git 仓库根目录"""
    stdout, stderr, code = run_command(["git", "rev-parse", "--show-toplevel"])
    if code != 0:
        console.print(f"[bold red]获取 Git 根目录失败:[/bold red] {stderr}")
        sys.exit(1)
    return stdout.strip()


def run_command(cmd: List[str], cwd: Optional[str] = None) -> Tuple[str, str, int]:
    """运行命令并返回输出"""
    try:
        # Windows 系统使用 GBK 编码
        encoding = "gbk" if sys.platform == "win32" else "utf-8"
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            encoding=encoding,
            errors="replace",  # 遇到无法解码的字符时替换为 ?
            cwd=cwd,
        )
        return result.stdout or "", result.stderr or "", result.returncode
    except Exception as e:
        return "", str(e), 1


def get_git_status() -> str:
    """获取 git 状态"""
    stdout, stderr, code = run_command(["git", "status"])
    if code != 0:
        console.print(f"[bold red]获取 git 状态失败:[/bold red] {stderr}")
        sys.exit(1)
    return stdout


def get_git_diff(staged: bool = False) -> str:
    """获取 git diff 信息"""
    cmd = ["git", "diff"]
    if staged:
        cmd.append("--cached")
    stdout, stderr, code = run_command(cmd)
    if code != 0:
        console.print(f"[bold red]获取 git diff 失败:[/bold red] {stderr}")
        sys.exit(1)
    return stdout


def get_git_diff_all() -> str:
    """获取所有 diff 信息（暂存和非暂存）"""
    staged_diff = get_git_diff(staged=True)
    unstaged_diff = get_git_diff(staged=False)

    result = []
    if staged_diff:
        result.append("=== 已暂存的更改 ===")
        result.append(staged_diff)
    if unstaged_diff:
        result.append("=== 未暂存的更改 ===")
        result.append(unstaged_diff)
    if not staged_diff and not unstaged_diff:
        result.append("没有检测到任何更改")

    return "\n".join(result)


def get_current_branch() -> str:
    """获取当前分支名"""
    stdout, stderr, code = run_command(["git", "rev-parse", "--abbrev-ref", "HEAD"])
    if code != 0:
        console.print(f"[bold yellow]获取当前分支失败:[/bold yellow] {stderr}")
        return "main"  # 默认回退到 main
    return stdout.strip()


def has_unpushed_commits() -> bool:
    """检查是否有未推送的提交"""
    current_branch = get_current_branch()
    # 检查 origin 远程分支
    run_command(["git", "fetch", "origin", "--quiet"])

    stdout, stderr, code = run_command(
        ["git", "rev-list", f"origin/{current_branch}..HEAD", "--count"]
    )

    if code == 0 and stdout.strip():
        try:
            unpushed_count = int(stdout.strip())
            if unpushed_count > 0:
                return True
        except ValueError:
            pass

    return False


def validate_commit_json(data: dict) -> bool:
    """验证提交信息 JSON 格式是否正确"""
    required_fields = ["type", "description"]
    optional_fields = ["scope", "body", "footer"]

    # 检查必需字段
    for field in required_fields:
        if field not in data or not isinstance(data[field], str) or not data[field].strip():
            return False

    # 检查可选字段类型
    for field in optional_fields:
        if field in data and data[field] is not None:
            # body 可以是字符串或数组
            if field == "body":
                if not isinstance(data[field], (str, list)):
                    return False
                if isinstance(data[field], list):
                    if not all(isinstance(item, str) for item in data[field]):
                        return False
            else:
                if not isinstance(data[field], str):
                    return False

    # 验证 type 是否为有效的提交类型
    valid_types = ["feat", "fix", "docs", "style", "refactor", "test", "chore", "perf", "ci", "build", "revert"]
    if data["type"] not in valid_types:
        return False

    return True


def format_commit_message(commit_data: dict) -> str:
    """将 JSON 格式的提交信息转换为标准提交信息"""
    # 构建第一行：type(scope): description
    first_line = commit_data["type"]
    if commit_data.get("scope"):
        first_line += f"({commit_data['scope']})"
    first_line += f": {commit_data['description']}"

    # 构建完整提交信息
    lines = [first_line]

    # 添加详细说明
    if commit_data.get("body"):
        lines.append("")  # 空行

        # 如果 body 是数组，转换为列表格式
        if isinstance(commit_data["body"], list):
            for item in commit_data["body"]:
                lines.append(f"- {item}")
        else:
            lines.append(commit_data["body"])

    # 添加页脚
    if commit_data.get("footer"):
        lines.append("")  # 空行
        lines.append(commit_data["footer"])

    return "\n".join(lines)


def generate_commit_message(diff_content: str) -> str:
    """调用大模型 API 生成提交信息（支持思维链模型，实时流式输出）"""
    if not API_KEY:
        console.print("[bold red]错误: 未设置 API_KEY，请检查 .env 文件[/bold red]")
        sys.exit(1)

    # 初始化 OpenAI 客户端
    client = OpenAI(api_key=API_KEY, base_url=API_BASE_URL)

    prompt = f"""请分析以下 git 改动，并按照 Conventional Commits 规范生成一个中文提交信息。

要求：
1. 使用 Conventional Commits 格式
2. 使用中文描述
3. 根据更改内容选择合适的 type（feat, fix, docs, style, refactor, test, chore, perf, ci, build, revert）
4. 如果有明确的作用域，填写 scope 字段
5. description 应简洁明了，概括主要改动
6. body 应详细列出主要的更改内容，使用数组格式，每个元素是一个要点
7. footer 用于备注破坏性变更或关闭的 issue（可选）

Git 改动信息：
{diff_content}

请以 JSON 格式返回，包含以下字段：
- type: 提交类型（必需）
- scope: 作用域（可选，如果没有则为 null）
- description: 简短描述（必需）
- body: 详细说明数组（可选，如果没有则为 null，如果有则为字符串数组，每个元素是一个要点）
- footer: 页脚信息（可选，如果没有则为 null）

示例格式：
{{
  "type": "feat",
  "scope": "api",
  "description": "添加用户认证功能",
  "body": [
    "实现 JWT 令牌生成和验证",
    "添加登录和注册接口",
    "集成密码加密功能"
  ],
  "footer": null
}}"""

    for attempt in range(MAX_RETRIES):
        try:
            console.print(f"[dim]正在调用模型 {MODEL_ID}...[/dim]")
            console.print()

            # 用于收集流式输出（只收集 content，不收集 reasoning）
            full_content = ""
            reasoning_displayed = False
            content_started = False

            # 调用 OpenAI API（流式输出）
            stream = client.chat.completions.create(
                model=MODEL_ID,
                messages=[
                    {"role": "system", "content": "你是一个专业的 Git 提交信息生成助手。请严格按照 JSON 格式返回结果。"},
                    {"role": "user", "content": prompt},
                ],
                temperature=0.3,
                max_completion_tokens=MAX_COMPLETION_TOKENS,
                response_format={"type": "json_object"},
                stream=True,
                stream_options={"include_usage": True}
            )

            # 实时显示输出
            console.print("[bold cyan]模型输出:[/bold cyan]")
            console.print("[dim]" + "─" * 60 + "[/dim]")

            for chunk in stream:
                if chunk.choices:
                    delta = chunk.choices[0].delta

                    if hasattr(delta, 'reasoning_content') and delta.reasoning_content:
                        if not reasoning_displayed:
                            console.print("[bold yellow]💭 思考过程:[/bold yellow]")
                            reasoning_displayed = True
                        console.print(delta.reasoning_content, end="", style="yellow dim")

                    if delta.content:
                        if not content_started:
                            if reasoning_displayed:
                                console.print()
                                console.print()
                            console.print("[bold green]📝 生成结果:[/bold green]")
                            content_started = True

                        full_content += delta.content
                        console.print(delta.content, end="", style="green")

                if hasattr(chunk, 'usage') and chunk.usage:
                    console.print()
                    console.print()
                    console.print(f"[dim]Token 使用: {chunk.usage.total_tokens} (输入: {chunk.usage.prompt_tokens}, 输出: {chunk.usage.completion_tokens})[/dim]")

            console.print()
            console.print("[dim]" + "─" * 60 + "[/dim]")
            console.print()

            if not full_content.strip():
                raise ValueError("模型未返回任何内容")

            try:
                commit_data = json.loads(full_content)
            except json.JSONDecodeError as e:
                console.print(f"[yellow]第 {attempt + 1} 次尝试: JSON 解析失败 - {e}[/yellow]")
                if attempt < MAX_RETRIES - 1:
                    time.sleep(1)
                    continue
                else:
                    raise

            if not validate_commit_json(commit_data):
                console.print(f"[yellow]第 {attempt + 1} 次尝试: JSON 格式验证失败[/yellow]")
                if attempt < MAX_RETRIES - 1:
                    time.sleep(1)
                    continue
                else:
                    raise ValueError("生成的提交信息格式不符合要求")

            commit_message = format_commit_message(commit_data)

            if attempt > 0:
                console.print(f"[green]✓ 第 {attempt + 1} 次尝试成功[/green]")
                console.print()

            return commit_message

        except Exception as e:
            console.print(f"[yellow]✗ 第 {attempt + 1} 次尝试失败: {e}[/yellow]")
            if attempt < MAX_RETRIES - 1:
                time.sleep(1)
            else:
                console.print(f"[bold red]调用大模型 API 失败（已重试 {MAX_RETRIES} 次）:[/bold red] {e}")
                sys.exit(1)

    console.print("[bold red]生成提交信息失败[/bold red]")
    sys.exit(1)


def git_add_all() -> bool:
    """添加所有更改到暂存区"""
    git_root = get_git_root()
    stdout, stderr, code = run_command(["git", "add", "."], cwd=git_root)
    if code != 0:
        console.print(f"[bold red]git add 失败:[/bold red] {stderr}")
        return False
    return True


def git_commit(message: str, user_info: Dict[str, str]) -> bool:
    """提交更改，使用指定的用户信息"""
    git_root = get_git_root()

    # 设置用户信息
    if user_info.get("name") and user_info.get("email"):
        run_command(["git", "config", "user.name", user_info["name"]], cwd=git_root)
        run_command(["git", "config", "user.email", user_info["email"]], cwd=git_root)

    encoding = "gbk" if sys.platform == "win32" else "utf-8"

    try:
        result = subprocess.run(
            ["git", "commit", "-m", message],
            capture_output=True,
            text=True,
            encoding=encoding,
            errors="replace",
            cwd=git_root,
        )

        if result.returncode != 0:
            error_msg = result.stderr or result.stdout or "未知错误"
            console.print(f"[bold red]git commit 失败:[/bold red] {error_msg.strip()}")
            return False

        success_msg = result.stdout or result.stderr or "提交成功"
        console.print(f"[bold green]提交成功:[/bold green] {success_msg.strip()}")
        return True
    except Exception as e:
        console.print(f"[bold red]git commit 失败:[/bold red] {e}")
        return False


def inject_token_to_url(url: str, token: str) -> str:
    """将 Token 注入到 HTTPS URL 中"""
    if not url.startswith("https://"):
        # 如果不是 HTTPS，暂时不处理或报错
        return url

    # 移除末尾的斜杠（如果有）
    url = url.rstrip('/')
    
    # https://github.com/user/repo.git -> https://token@github.com/user/repo.git
    # 解析 URL
    parsed = urlparse(url)
    
    # 如果 netloc 中已经包含认证信息（user@host），先移除
    netloc = parsed.netloc
    if '@' in netloc:
        # 移除已有的认证信息，只保留 hostname
        netloc = netloc.split('@')[-1]
    
    # 构造新的 netloc: token@github.com
    new_netloc = f"{token}@{netloc}"
    # 重新构造 URL
    new_url = urlunparse(parsed._replace(netloc=new_netloc))
    return new_url


def sanitize_output(text: str, token: str) -> str:
    """从输出文本中移除敏感的 Token 信息"""
    if token and token in text:
        return text.replace(token, "******")
    return text


def git_push_to_github(user_info: Dict[str, str]) -> bool:
    """
    推送到 GitHub 远程仓库
    使用 Token 进行认证，避免交互式登录
    """
    git_root = get_git_root()
    current_branch = get_current_branch()
    token = user_info.get("token", "")

    # 检查远程配置
    stdout, stderr, code = run_command(["git", "remote", "get-url", "origin"])
    current_remote_url = stdout.strip().rstrip('/')

    # 如果 origin 不存在或 URL 不匹配配置，则更新
    if code != 0 or not current_remote_url:
        clean_remote_origin = REMOTE_ORIGIN.rstrip('/')
        console.print(f"[cyan]设置远程仓库 origin 为: {clean_remote_origin}[/cyan]")
        if code != 0:
            run_command(["git", "remote", "add", "origin", clean_remote_origin], cwd=git_root)
        else:
            run_command(["git", "remote", "set-url", "origin", clean_remote_origin], cwd=git_root)
        current_remote_url = clean_remote_origin

    # 构造带 Token 的 URL
    auth_url = inject_token_to_url(current_remote_url, token) if token else current_remote_url

    push_retries = int(os.getenv("PUSH_RETRIES", "3"))
    retry_delay = int(os.getenv("RETRY_DELAY", "2"))

    console.print(
        f"[cyan]正在推送到 GitHub (分支: {current_branch})...[/cyan]"
    )

    # 临时设置带 Token 的 URL 用于推送
    if token:
        run_command(["git", "remote", "set-url", "origin", auth_url], cwd=git_root)

    push_success = False
    for attempt in range(push_retries):
        if attempt > 0:
            console.print(f"[yellow]第 {attempt + 1} 次重试推送...[/yellow]")
            time.sleep(retry_delay)

        # 使用 -u 设置上游分支
        stdout, stderr, code = run_command(
            ["git", "push", "-u", "origin", current_branch],
            cwd=git_root
        )

        # 脱敏输出
        safe_stdout = sanitize_output(stdout, token)
        safe_stderr = sanitize_output(stderr, token)

        if code == 0:
            console.print(f"[bold green]✓ 推送成功[/bold green]")
            if safe_stdout: console.print(safe_stdout)
            push_success = True
            break
        else:
            error_msg = safe_stderr or safe_stdout or "未知错误"
            is_network_error = any(kw in error_msg.lower() for kw in [
                "ssl", "tls", "connection", "timeout", "network", "could not resolve"
            ])

            if attempt < push_retries - 1:
                if is_network_error:
                    console.print(f"[yellow]✗ 推送失败（网络错误）:[/yellow] {error_msg.strip()}")
                    console.print(f"[dim]等待 {retry_delay} 秒后重试...[/dim]")
                else:
                    # 非 network 错误（如 Token 权限不足、分支冲突等），直接报错退出
                    console.print(f"[bold red]✗ 推送失败:[/bold red] {error_msg}")
                    break
            else:
                console.print(f"[bold red]✗ 推送失败（已重试 {push_retries} 次）:[/bold red] {error_msg}")

    # 推送结束后，无论成功与否，都将 URL 改回不含 Token 的形式，保证安全
    if token:
        run_command(["git", "remote", "set-url", "origin", current_remote_url], cwd=git_root)

    return push_success


def select_git_user() -> Dict[str, str]:
    """让用户选择要使用的 Git 用户配置"""
    if not GIT_USERS:
        console.print("[bold red]错误: 未配置 GIT_USERS 或配置为空，请检查 .env 文件[/bold red]")
        sys.exit(1)

    if len(GIT_USERS) == 1:
        console.print(f"[cyan]使用默认用户: {GIT_USERS[0]['name']} <{GIT_USERS[0]['email']}>[/cyan]")
        return GIT_USERS[0]

    console.print("[bold cyan]请选择提交用户:[/bold cyan]")
    table = Table(show_header=True, header_style="bold magenta")
    table.add_column("序号", style="cyan", justify="center")
    table.add_column("用户名")
    table.add_column("邮箱")

    for idx, user in enumerate(GIT_USERS):
        table.add_row(str(idx + 1), user.get("name", "N/A"), user.get("email", "N/A"))

    console.print(table)

    while True:
        choice = Prompt.ask(
            "请输入序号",
            default="1",
            show_default=True
        )
        try:
            idx = int(choice) - 1
            if 0 <= idx < len(GIT_USERS):
                return GIT_USERS[idx]
            else:
                console.print("[red]序号无效，请重新输入[/red]")
        except ValueError:
            console.print("[red]请输入有效的数字序号[/red]")


def main():
    """主函数"""
    # 显示标题
    console.print(
        Panel.fit(
            "[bold cyan]CocoMonyaB 一键提交工具[/bold cyan]",
            border_style="cyan",
            padding=(1, 2),
        )
    )

    # 校验配置
    if not REMOTE_ORIGIN or "github.com" not in REMOTE_ORIGIN:
         console.print(
            Panel(
                f"[bold red]错误: REMOTE_ORIGIN 配置无效[/bold red]\n\n当前配置: {REMOTE_ORIGIN}\n必须在 .env 中配置有效的 GitHub 仓库地址。",
                border_style="red",
                title="配置错误",
            )
        )
         sys.exit(1)

    console.print()

    # 1. 检查 git 仓库
    with console.status("[bold cyan]检查 Git 仓库...[/bold cyan]", spinner="dots"):
        stdout, stderr, code = run_command(
            ["git", "rev-parse", "--is-inside-work-tree"]
        )

    if code != 0:
        console.print(
            Panel(
                "[bold red]错误: 当前目录不是 Git 仓库[/bold red]\n\n请确保在当前 Git 仓库目录中运行此工具。",
                border_style="red",
                title="❌ 错误",
            )
        )
        sys.exit(1)

    # 2. 选择用户
    console.print("[bold cyan]1. 选择 Git 用户[/bold cyan]")
    selected_user = select_git_user()
    console.print(f"[green]✓ 已选择用户: {selected_user['name']}[/green]")
    console.print()

    # 3. 获取当前状态
    console.print("[bold cyan]2. 检查 Git 状态[/bold cyan]")
    with console.status("[dim]获取 Git 状态信息...[/dim]", spinner="dots"):
        status = get_git_status()

    console.print(
        Panel(
            Syntax(status, "bash", theme="monokai", line_numbers=False),
            border_style="blue",
            title="Git 状态",
            padding=(0, 1),
        )
    )

    console.print()

    # 4. 获取 diff 信息
    console.print("[bold cyan]3. 分析改动内容[/bold cyan]")
    with console.status("[dim]获取 Git diff 信息...[/dim]", spinner="dots"):
        diff_content = get_git_diff_all()

    if "没有检测到任何更改" in diff_content:
        console.print("[yellow]没有未提交的更改[/yellow]")
        # 检查是否有未推送的提交
        console.print()
        with console.status("[dim]检查未推送的提交...[/dim]", spinner="dots"):
            unpushed = has_unpushed_commits()

        if unpushed:
            console.print(
                Panel(
                    "[bold yellow]检测到有未推送的本地提交，自动推送到 GitHub...[/bold yellow]",
                    border_style="yellow",
                    title="检测到未推送提交",
                )
            )
            if git_push_to_github(selected_user):
                console.print(
                    Panel(
                        "[bold green]✓ 所有提交已成功推送![/bold green]",
                        border_style="green",
                        title="完成",
                    )
                )
            else:
                console.print(
                    Panel(
                        "[bold red]! 推送过程中出现错误[/bold red]",
                        border_style="red",
                        title="错误",
                    )
                )
            return

        console.print(
            Panel(
                "[dim]没有需要提交或推送的更改[/dim]",
                border_style="dim",
                title="无更改",
            )
        )
        return

    # 显示 diff 信息
    diff_lines = len(diff_content.splitlines())
    console.print(
        Panel(
            f"[bold]检测到 {diff_lines} 行改动[/bold]\n\n[dim]正在分析改动内容...[/dim]",
            border_style="cyan",
            title="代码改动",
        )
    )

    # 5. 生成提交信息
    console.print()
    console.print("[bold cyan]4. 生成提交信息[/bold cyan]")
    console.print()
    commit_message = generate_commit_message(diff_content)

    # 显示生成的提交信息
    console.print(
        Panel(
            Syntax(commit_message, "text", theme="monokai"),
            border_style="green",
            title="生成的提交信息",
            padding=(1, 2),
        )
    )

    console.print()

    # 6. 确认是否继续
    console.print("[bold cyan]5. 确认提交并推送[/bold cyan]")
    if not Confirm.ask("是否继续提交并推送到 GitHub？", default=True):
        console.print("[yellow]取消操作[/yellow]")
        return

    # 7. 添加所有更改
    console.print()
    console.print("[bold cyan]6. 添加更改到暂存区[/bold cyan]")
    with console.status("[dim]执行 git add .[/dim]", spinner="dots"):
        if not git_add_all():
            sys.exit(1)

    console.print("[green]✓ 所有更改已添加到暂存区[/green]")

    # 8. 提交
    console.print()
    console.print("[bold cyan]7. 提交更改[/bold cyan]")
    with console.status("[dim]执行 git commit[/dim]", spinner="dots"):
        if not git_commit(commit_message, selected_user):
            sys.exit(1)

    # 9. 推送
    console.print()
    console.print("[bold cyan]8. 推送到 GitHub[/bold cyan]")
    if not git_push_to_github(selected_user):
        console.print(
            Panel(
                "[bold yellow]警告: 推送过程中出现错误，但提交已成功[/bold yellow]",
                border_style="yellow",
                title="部分错误",
            )
        )
    else:
        console.print("[green]✓ 所有提交已成功推送[/green]")

    # 完成
    console.print()
    console.print(
        Panel(
            "[bold green]✓ 所有操作已完成![/bold green]\n\n"
            "[dim]提交信息已生成并推送至 GitHub。[/dim]",
            border_style="green",
            title="完成",
            padding=(1, 2),
        )
    )


if __name__ == "__main__":
    main()
