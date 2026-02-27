#!/usr/bin/env python3
"""
版本发布工具
将 development 分支使用 Squash and merge 模式合并到 main 分支，并推送标签
支持自动构建 JAR 包和上传 GitHub Release
支持多 Git 用户配置及 Token 认证推送
"""

import os

# 设置 UTF-8 编码
os.environ["PYTHONUTF8"] = "1"
os.environ["PYTHONIOENCODING"] = "utf-8"
import sys
import subprocess
import json
import re
import time
import requests
from pathlib import Path
from typing import List, Tuple, Optional, Dict
from dotenv import load_dotenv
from rich.console import Console
from rich.panel import Panel
from rich.table import Table
from rich.prompt import Prompt, Confirm
from rich.syntax import Syntax
from rich.progress import Progress, SpinnerColumn, TextColumn, BarColumn, TaskProgressColumn
from urllib.parse import urlparse, urlunparse

# 加载环境变量
env_path = Path(__file__).parent / ".env"
load_dotenv(dotenv_path=env_path)

# 远程仓库配置 (仅支持 GitHub)
REMOTE_ORIGIN = os.getenv("REMOTE_ORIGIN", "")

# 解析多用户配置
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


def get_current_branch() -> str:
    """获取当前分支名"""
    stdout, stderr, code = run_command(["git", "rev-parse", "--abbrev-ref", "HEAD"])
    if code != 0:
        console.print(f"[bold red]获取当前分支失败:[/bold red] {stderr}")
        sys.exit(1)
    return stdout.strip()


def check_working_tree_clean() -> bool:
    """检查工作区是否干净（没有未提交的更改）"""
    stdout, stderr, code = run_command(["git", "status", "--porcelain"])
    if code != 0:
        console.print(f"[bold red]检查工作区状态失败:[/bold red] {stderr}")
        return False
    return len(stdout.strip()) == 0


def get_all_branches() -> List[str]:
    """获取所有本地分支"""
    stdout, stderr, code = run_command(["git", "branch", "--format=%(refname:short)"])
    if code != 0:
        console.print(f"[bold red]获取分支列表失败:[/bold red] {stderr}")
        return []
    return [branch.strip() for branch in stdout.strip().split("\n") if branch.strip()]


def fetch_remote(user_info: Dict[str, str]) -> bool:
    """从远程仓库获取最新信息"""
    git_root = get_git_root()
    token = user_info.get("token", "")
    
    # 获取当前远程 URL
    stdout, stderr, code = run_command(["git", "remote", "get-url", "origin"])
    current_remote_url = stdout.strip()
    
    # 构造带 Token 的 URL
    auth_url = inject_token_to_url(current_remote_url, token) if token else current_remote_url
    
    # 临时设置带 Token 的 URL
    if token:
        run_command(["git", "remote", "set-url", "origin", auth_url], cwd=git_root)
    
    console.print("[cyan]正在从远程仓库获取最新信息...[/cyan]")
    stdout, stderr, code = run_command(["git", "fetch", "origin"], cwd=git_root)
    
    # 恢复原始 URL
    if token:
        run_command(["git", "remote", "set-url", "origin", current_remote_url], cwd=git_root)
    
    if code != 0:
        safe_stderr = sanitize_output(stderr, token)
        console.print(f"[bold red]获取远程信息失败:[/bold red] {safe_stderr}")
        return False
    
    console.print("[green]✓ 远程信息已更新[/green]")
    return True


def check_branch_up_to_date(branch: str) -> bool:
    """检查本地分支是否与远程分支同步"""
    # 检查远程分支是否存在
    stdout, stderr, code = run_command(["git", "rev-parse", f"origin/{branch}"])
    if code != 0:
        console.print(f"[yellow]警告: 远程分支 origin/{branch} 不存在[/yellow]")
        return True  # 如果远程分支不存在，认为是新分支，允许继续
    
    # 比较本地和远程分支
    stdout_local, _, code_local = run_command(["git", "rev-parse", branch])
    stdout_remote, _, code_remote = run_command(["git", "rev-parse", f"origin/{branch}"])
    
    if code_local != 0 or code_remote != 0:
        return False
    
    return stdout_local.strip() == stdout_remote.strip()


def read_version_from_gradle() -> Optional[str]:
    """从 build.gradle.kts 读取版本号"""
    git_root = get_git_root()
    gradle_file = Path(git_root) / "build.gradle.kts"
    
    if not gradle_file.exists():
        console.print(f"[bold red]错误: 找不到 build.gradle.kts 文件[/bold red]")
        return None
    
    try:
        content = gradle_file.read_text(encoding="utf-8")
        # 匹配 version = "x.x.x" 格式
        match = re.search(r'version\s*=\s*"([^"]+)"', content)
        if match:
            return match.group(1)
        else:
            console.print("[bold red]错误: 无法从 build.gradle.kts 中解析版本号[/bold red]")
            return None
    except Exception as e:
        console.print(f"[bold red]读取 build.gradle.kts 失败:[/bold red] {e}")
        return None


def inject_token_to_url(url: str, token: str) -> str:
    """将 Token 注入到 HTTPS URL 中"""
    if not url.startswith("https://"):
        return url
    
    parsed = urlparse(url)
    new_netloc = f"{token}@{parsed.netloc}"
    new_url = urlunparse(parsed._replace(netloc=new_netloc))
    return new_url


def sanitize_output(text: str, token: str) -> str:
    """从输出文本中移除敏感的 Token 信息"""
    if token and token in text:
        return text.replace(token, "******")
    return text


def git_push_with_token(branch: str, user_info: Dict[str, str], tags: bool = False) -> bool:
    """推送分支或标签到远程仓库"""
    git_root = get_git_root()
    token = user_info.get("token", "")
    
    # 获取当前远程 URL
    stdout, stderr, code = run_command(["git", "remote", "get-url", "origin"])
    current_remote_url = stdout.strip()
    
    # 构造带 Token 的 URL
    auth_url = inject_token_to_url(current_remote_url, token) if token else current_remote_url
    
    # 临时设置带 Token 的 URL
    if token:
        run_command(["git", "remote", "set-url", "origin", auth_url], cwd=git_root)
    
    # 构建推送命令
    if tags:
        cmd = ["git", "push", "origin", "--tags"]
        console.print("[cyan]正在推送标签...[/cyan]")
    else:
        cmd = ["git", "push", "origin", branch]
        console.print(f"[cyan]正在推送分支 {branch}...[/cyan]")
    
    stdout, stderr, code = run_command(cmd, cwd=git_root)
    
    # 恢复原始 URL
    if token:
        run_command(["git", "remote", "set-url", "origin", current_remote_url], cwd=git_root)
    
    # 脱敏输出
    safe_stdout = sanitize_output(stdout, token)
    safe_stderr = sanitize_output(stderr, token)
    
    if code == 0:
        console.print(f"[bold green]✓ 推送成功[/bold green]")
        if safe_stdout:
            console.print(safe_stdout)
        return True
    else:
        error_msg = safe_stderr or safe_stdout or "未知错误"
        console.print(f"[bold red]✗ 推送失败:[/bold red] {error_msg}")
        return False


def build_jar() -> Optional[Path]:
    """构建 JAR 包"""
    git_root = get_git_root()
    console.print("[bold cyan]构建 JAR 包[/bold cyan]")
    console.print("[cyan]执行: ./gradlew clean bootJar[/cyan]")
    console.print()
    
    # 使用 gradlew 构建
    if sys.platform == "win32":
        gradlew_cmd = [str(Path(git_root) / "gradlew.bat"), "clean", "bootJar"]
    else:
        gradlew_cmd = ["./gradlew", "clean", "bootJar"]
    
    with Progress(
        SpinnerColumn(),
        TextColumn("[progress.description]{task.description}"),
        console=console,
    ) as progress:
        task = progress.add_task("[cyan]正在构建...", total=None)
        
        # Windows 系统使用 GBK 编码
        encoding = "gbk" if sys.platform == "win32" else "utf-8"
        
        try:
            result = subprocess.run(
                gradlew_cmd,
                capture_output=True,
                text=True,
                encoding=encoding,
                errors="replace",
                cwd=git_root,
                shell=(sys.platform == "win32"),
            )
            
            progress.update(task, completed=True)
            
            if result.returncode != 0:
                console.print("[bold red]✗ 构建失败[/bold red]")
                console.print()
                console.print("[bold red]错误输出:[/bold red]")
                console.print(result.stderr or result.stdout)
                return None
            
            console.print("[bold green]✓ 构建成功[/bold green]")
            
            # 查找生成的 JAR 文件
            build_libs = Path(git_root) / "build" / "libs"
            if not build_libs.exists():
                console.print("[bold red]错误: build/libs 目录不存在[/bold red]")
                return None
            
            # 查找 bootJar 生成的文件（通常不包含 -plain）
            jar_files = [f for f in build_libs.glob("*.jar") if "-plain" not in f.name]
            
            if not jar_files:
                console.print("[bold red]错误: 未找到 JAR 文件[/bold red]")
                return None
            
            jar_file = jar_files[0]
            console.print(f"[green]JAR 文件: {jar_file.name}[/green]")
            console.print(f"[dim]大小: {jar_file.stat().st_size / 1024 / 1024:.2f} MB[/dim]")
            
            return jar_file
            
        except Exception as e:
            console.print(f"[bold red]构建过程出错:[/bold red] {e}")
            return None


def extract_repo_info(remote_url: str) -> Optional[Tuple[str, str]]:
    """从远程 URL 提取仓库所有者和名称"""
    # 支持 HTTPS 和 SSH 格式
    # https://github.com/owner/repo.git
    # git@github.com:owner/repo.git
    
    if "github.com" not in remote_url:
        return None
    
    # HTTPS 格式
    match = re.search(r"github\.com[:/]([^/]+)/([^/]+?)(?:\.git)?$", remote_url)
    if match:
        return match.group(1), match.group(2)
    
    return None


def create_github_release(
    version: str,
    tag_name: str,
    jar_file: Path,
    user_info: Dict[str, str]
) -> bool:
    """创建 GitHub Release 并上传 JAR 包"""
    token = user_info.get("token", "")
    if not token:
        console.print("[bold red]错误: 未配置 GitHub Token[/bold red]")
        return False
    
    # 提取仓库信息
    repo_info = extract_repo_info(REMOTE_ORIGIN)
    if not repo_info:
        console.print("[bold red]错误: 无法从 REMOTE_ORIGIN 提取仓库信息[/bold red]")
        return False
    
    owner, repo = repo_info
    console.print(f"[cyan]仓库: {owner}/{repo}[/cyan]")
    
    # GitHub API 端点
    api_base = "https://api.github.com"
    releases_url = f"{api_base}/repos/{owner}/{repo}/releases"
    
    headers = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github.v3+json",
    }
    
    # 1. 创建 Release
    console.print("[cyan]创建 GitHub Release...[/cyan]")
    
    release_data = {
        "tag_name": tag_name,
        "name": f"Release {version}",
        "body": f"## 版本 {version}\n\n自动发布",
        "draft": False,
        "prerelease": "-" in version,  # 如果版本号包含 - 则标记为预发布
    }
    
    try:
        response = requests.post(releases_url, headers=headers, json=release_data)
        
        if response.status_code == 201:
            release = response.json()
            console.print(f"[green]✓ Release 创建成功[/green]")
            console.print(f"[dim]URL: {release['html_url']}[/dim]")
        elif response.status_code == 422:
            # Release 已存在，获取现有 Release
            console.print("[yellow]Release 已存在，获取现有 Release...[/yellow]")
            get_response = requests.get(f"{releases_url}/tags/{tag_name}", headers=headers)
            
            if get_response.status_code == 200:
                release = get_response.json()
                console.print(f"[green]✓ 使用现有 Release[/green]")
            else:
                console.print(f"[bold red]获取 Release 失败: {get_response.status_code}[/bold red]")
                console.print(get_response.text)
                return False
        else:
            console.print(f"[bold red]创建 Release 失败: {response.status_code}[/bold red]")
            console.print(response.text)
            return False
        
        # 2. 上传 JAR 文件
        console.print()
        console.print("[cyan]上传 JAR 文件...[/cyan]")
        
        upload_url = release["upload_url"].replace("{?name,label}", "")
        file_size = jar_file.stat().st_size
        
        with open(jar_file, "rb") as f:
            file_data = f.read()
        
        upload_headers = {
            "Authorization": f"token {token}",
            "Content-Type": "application/java-archive",
        }
        
        upload_params = {
            "name": jar_file.name,
        }
        
        with Progress(
            SpinnerColumn(),
            TextColumn("[progress.description]{task.description}"),
            BarColumn(),
            TaskProgressColumn(),
            console=console,
        ) as progress:
            task = progress.add_task(
                f"[cyan]上传 {jar_file.name}...",
                total=100
            )
            
            # 简单上传（对于小文件）
            upload_response = requests.post(
                upload_url,
                headers=upload_headers,
                params=upload_params,
                data=file_data,
            )
            
            progress.update(task, completed=100)
        
        if upload_response.status_code == 201:
            asset = upload_response.json()
            console.print(f"[bold green]✓ JAR 文件上传成功[/bold green]")
            console.print(f"[dim]下载链接: {asset['browser_download_url']}[/dim]")
            return True
        else:
            console.print(f"[bold red]上传失败: {upload_response.status_code}[/bold red]")
            console.print(upload_response.text)
            return False
            
    except Exception as e:
        console.print(f"[bold red]创建 Release 或上传文件时出错:[/bold red] {e}")
        return False


def select_git_user() -> Dict[str, str]:
    """让用户选择要使用的 Git 用户配置"""
    if not GIT_USERS:
        console.print("[bold red]错误: 未配置 GIT_USERS 或配置为空，请检查 .env 文件[/bold red]")
        sys.exit(1)
    
    if len(GIT_USERS) == 1:
        console.print(f"[cyan]使用默认用户: {GIT_USERS[0]['name']} <{GIT_USERS[0]['email']}>[/cyan]")
        return GIT_USERS[0]
    
    console.print("[bold cyan]请选择推送用户:[/bold cyan]")
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
            "[bold cyan]CocoMonyaB 版本发布工具[/bold cyan]\n"
            "[dim]构建 JAR → 合并分支 → 推送标签 → 上传 Release[/dim]",
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
    console.print("[bold cyan]1. 检查 Git 仓库[/bold cyan]")
    stdout, stderr, code = run_command(["git", "rev-parse", "--is-inside-work-tree"])
    
    if code != 0:
        console.print(
            Panel(
                "[bold red]错误: 当前目录不是 Git 仓库[/bold red]\n\n请确保在 Git 仓库目录中运行此工具。",
                border_style="red",
                title="❌ 错误",
            )
        )
        sys.exit(1)
    
    console.print("[green]✓ Git 仓库检查通过[/green]")
    console.print()
    
    # 2. 构建 JAR 包（在所有检查之前）
    console.print("[bold cyan]2. 构建 JAR 包[/bold cyan]")
    jar_file = build_jar()
    
    if not jar_file:
        console.print(
            Panel(
                "[bold red]构建失败，操作终止[/bold red]\n\n"
                "请检查构建错误并修复后重试。",
                border_style="red",
                title="❌ 构建失败",
            )
        )
        sys.exit(1)
    
    console.print()
    
    # 保存 JAR 文件路径供后续使用
    jar_file_path = jar_file
    
    # 3. 选择用户
    console.print("[bold cyan]3. 选择 Git 用户[/bold cyan]")
    selected_user = select_git_user()
    console.print(f"[green]✓ 已选择用户: {selected_user['name']}[/green]")
    console.print()
    
    # 4. 获取远程最新信息
    console.print("[bold cyan]4. 获取远程仓库信息[/bold cyan]")
    if not fetch_remote(selected_user):
        console.print("[bold red]无法获取远程信息，请检查网络连接和权限[/bold red]")
        sys.exit(1)
    console.print()
    
    # 5. 检查所有分支工作区是否干净
    console.print("[bold cyan]5. 检查工作区状态[/bold cyan]")
    current_branch = get_current_branch()
    console.print(f"[dim]当前分支: {current_branch}[/dim]")
    
    if not check_working_tree_clean():
        console.print(
            Panel(
                "[bold red]错误: 工作区不干净[/bold red]\n\n"
                "检测到未提交的更改。请先提交或暂存所有更改后再运行此工具。\n\n"
                "提示: 可以使用 'git status' 查看未提交的更改",
                border_style="red",
                title="❌ 工作区检查失败",
            )
        )
        sys.exit(1)
    
    console.print("[green]✓ 工作区干净[/green]")
    console.print()
    
    # 6. 检查 development 分支是否存在且是最新的
    console.print("[bold cyan]6. 检查 development 分支[/bold cyan]")
    all_branches = get_all_branches()
    
    if "development" not in all_branches:
        console.print(
            Panel(
                "[bold red]错误: development 分支不存在[/bold red]\n\n"
                "请确保本地存在 development 分支。",
                border_style="red",
                title="❌ 分支检查失败",
            )
        )
        sys.exit(1)
    
    # 切换到 development 分支检查
    if current_branch != "development":
        console.print("[cyan]切换到 development 分支...[/cyan]")
        stdout, stderr, code = run_command(["git", "checkout", "development"])
        if code != 0:
            console.print(f"[bold red]切换到 development 分支失败:[/bold red] {stderr}")
            sys.exit(1)
    
    # 检查 development 是否与远程同步
    if not check_branch_up_to_date("development"):
        console.print(
            Panel(
                "[bold red]错误: development 分支不是最新的[/bold red]\n\n"
                "本地 development 分支与远程不同步。\n"
                "请先执行 'git pull origin development' 更新分支。",
                border_style="red",
                title="❌ 分支同步检查失败",
            )
        )
        sys.exit(1)
    
    console.print("[green]✓ development 分支是最新的[/green]")
    console.print()
    
    # 7. 读取版本号
    console.print("[bold cyan]7. 读取版本号[/bold cyan]")
    version = read_version_from_gradle()
    
    if not version:
        console.print("[bold red]无法读取版本号，操作终止[/bold red]")
        sys.exit(1)
    
    console.print(
        Panel(
            f"[bold green]版本号: {version}[/bold green]",
            border_style="green",
            title="📦 当前版本",
        )
    )
    console.print()
    
    # 8. 确认操作
    console.print("[bold cyan]8. 确认发布操作[/bold cyan]")
    console.print(
        Panel(
            f"[bold yellow]即将执行以下操作:[/bold yellow]\n\n"
            f"1. 切换到 main 分支\n"
            f"2. 使用 Squash and merge 模式合并 development 分支\n"
            f"3. 推送 main 分支到远程仓库\n"
            f"4. 创建标签 v{version}\n"
            f"5. 推送标签到远程仓库\n"
            f"6. 创建 GitHub Release 并上传 JAR 包\n"
            f"7. 切换回 development 分支\n\n"
            f"[dim]注意: development 分支将被保留[/dim]",
            border_style="yellow",
            title="⚠️  操作确认",
        )
    )
    
    if not Confirm.ask("是否继续？", default=False):
        console.print("[yellow]操作已取消[/yellow]")
        sys.exit(0)
    
    console.print()
    
    # 9. 切换到 main 分支
    console.print("[bold cyan]9. 切换到 main 分支[/bold cyan]")
    
    # 检查 main 分支是否存在
    if "main" not in all_branches:
        console.print("[yellow]main 分支不存在，将创建新分支[/yellow]")
        stdout, stderr, code = run_command(["git", "checkout", "-b", "main"])
    else:
        stdout, stderr, code = run_command(["git", "checkout", "main"])
    
    if code != 0:
        console.print(f"[bold red]切换到 main 分支失败:[/bold red] {stderr}")
        sys.exit(1)
    
    console.print("[green]✓ 已切换到 main 分支[/green]")
    console.print()
    
    # 10. 合并 development 分支（Squash and merge）
    console.print("[bold cyan]10. 合并 development 分支[/bold cyan]")
    console.print("[cyan]使用 Squash and merge 模式...[/cyan]")
    
    # 执行 squash merge，允许不相关的历史记录
    stdout, stderr, code = run_command(["git", "merge", "--squash", "--allow-unrelated-histories", "development"])
    
    if code != 0:
        error_msg = stderr or stdout or "未知错误"
        console.print(f"[bold red]合并失败:[/bold red] {error_msg}")
        if stdout:
            console.print(f"[dim]输出:[/dim] {stdout}")
        
        # 检查是否有冲突
        status_stdout, _, status_code = run_command(["git", "status", "--porcelain"])
        if status_code == 0 and "both added" in status_stdout:
            console.print("[yellow]检测到合并冲突，尝试自动解决...[/yellow]")
            
            # 对于 both added 的文件，使用 development 分支的版本
            for line in status_stdout.split("\n"):
                if "both added" in line or "AA" in line[:2]:
                    file_path = line.split()[-1]
                    console.print(f"[cyan]解决冲突: {file_path} (使用 development 版本)[/cyan]")
                    run_command(["git", "checkout", "--theirs", file_path])
                    run_command(["git", "add", file_path])
            
            console.print("[green]✓ 冲突已自动解决[/green]")
        else:
            console.print("[yellow]正在恢复到 development 分支...[/yellow]")
            run_command(["git", "reset", "--hard"])
            run_command(["git", "checkout", "development"])
            sys.exit(1)
    
    console.print("[green]✓ 合并完成（暂存区）[/green]")
    
    # 提交合并
    commit_message = f"chore(release): 发布版本 {version}\n\n合并 development 分支的所有更改"
    stdout, stderr, code = run_command(["git", "commit", "-m", commit_message])
    
    if code != 0:
        console.print(f"[bold red]提交失败:[/bold red] {stderr}")
        console.print("[yellow]正在恢复到 development 分支...[/yellow]")
        run_command(["git", "reset", "--hard"])
        run_command(["git", "checkout", "development"])
        sys.exit(1)
    
    console.print("[green]✓ 合并已提交[/green]")
    console.print()
    
    # 11. 推送 main 分支
    console.print("[bold cyan]11. 推送 main 分支[/bold cyan]")
    if not git_push_with_token("main", selected_user):
        console.print("[bold red]推送 main 分支失败[/bold red]")
        console.print("[yellow]正在恢复到 development 分支...[/yellow]")
        run_command(["git", "checkout", "development"])
        sys.exit(1)
    console.print()
    
    # 12. 创建标签
    console.print("[bold cyan]12. 创建版本标签[/bold cyan]")
    tag_name = f"v{version}"
    tag_message = f"Release version {version}"
    
    # 检查标签是否已存在
    stdout, stderr, code = run_command(["git", "tag", "-l", tag_name])
    if stdout.strip() == tag_name:
        console.print(f"[yellow]警告: 标签 {tag_name} 已存在[/yellow]")
        if Confirm.ask("是否删除现有标签并重新创建？", default=False):
            run_command(["git", "tag", "-d", tag_name])
            console.print(f"[cyan]已删除本地标签 {tag_name}[/cyan]")
        else:
            console.print("[yellow]跳过标签创建和 Release 上传[/yellow]")
            console.print()
            console.print("[bold cyan]13. 切换回 development 分支[/bold cyan]")
            run_command(["git", "checkout", "development"])
            console.print("[green]✓ 已切换回 development 分支[/green]")
            console.print()
            console.print(
                Panel(
                    "[bold green]✓ 发布完成（未创建新标签）[/bold green]",
                    border_style="green",
                    title="完成",
                )
            )
            return
    
    stdout, stderr, code = run_command(["git", "tag", "-a", tag_name, "-m", tag_message])
    
    if code != 0:
        console.print(f"[bold red]创建标签失败:[/bold red] {stderr}")
        console.print("[yellow]正在切换回 development 分支...[/yellow]")
        run_command(["git", "checkout", "development"])
        sys.exit(1)
    
    console.print(f"[green]✓ 已创建标签 {tag_name}[/green]")
    console.print()
    
    # 13. 推送标签
    console.print("[bold cyan]13. 推送标签[/bold cyan]")
    if not git_push_with_token("", selected_user, tags=True):
        console.print("[bold red]推送标签失败[/bold red]")
        console.print("[yellow]正在切换回 development 分支...[/yellow]")
        run_command(["git", "checkout", "development"])
        sys.exit(1)
    console.print()
    
    # 14. 创建 GitHub Release 并上传 JAR 包
    console.print("[bold cyan]14. 创建 GitHub Release 并上传 JAR 包[/bold cyan]")
    if not create_github_release(version, tag_name, jar_file_path, selected_user):
        console.print("[yellow]警告: 创建 Release 或上传 JAR 包失败[/yellow]")
        console.print("[dim]可以稍后手动在 GitHub 上创建 Release 并上传文件[/dim]")
    console.print()
    
    # 15. 切换回 development 分支
    console.print("[bold cyan]15. 切换回 development 分支[/bold cyan]")
    stdout, stderr, code = run_command(["git", "checkout", "development"])
    
    if code != 0:
        console.print(f"[bold red]切换回 development 分支失败:[/bold red] {stderr}")
        console.print("[yellow]请手动切换回 development 分支[/yellow]")
    else:
        console.print("[green]✓ 已切换回 development 分支[/green]")
    
    console.print()
    
    # 完成
    console.print(
        Panel(
            f"[bold green]✓ 版本 {version} 发布完成！[/bold green]\n\n"
            f"[dim]已完成以下操作:[/dim]\n"
            f"• 构建 JAR 包: {jar_file_path.name}\n"
            f"• 将 development 分支合并到 main 分支\n"
            f"• 推送 main 分支到远程仓库\n"
            f"• 创建并推送标签 {tag_name}\n"
            f"• 创建 GitHub Release 并上传 JAR 包\n"
            f"• 切换回 development 分支",
            border_style="green",
            title="🎉 发布成功",
            padding=(1, 2),
        )
    )


if __name__ == "__main__":
    main()
