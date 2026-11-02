[toc]

# 第三题：Hermes CLI 本地安装与 DeepSeek 实操记录

> 开始日期：2026-10-28。
>
> 操作方式：最初由用户执行、AI 指导；用户随后明确授权 AI 代为卸载旧安装、配置路径并安装新版本。本文依据实际反馈和执行结果记录状态。
>
> 当前进度：旧安装清理、Work-Data 路径配置和新安装均已完成，版本命令及包管理检查通过。DeepSeek 模型配置已据用户截图确认保存；首次真实对话已通过，界面显示 deepseek-flash 并返回预期回复。文件写入与读取实验也已通过独立文件检查；跨会话 Memory 以及 Skill 创建、加载和文件任务复用实验均已通过。

## 一、实践目标与范围

在本地 Mac 安装 Hermes CLI，连接 DeepSeek，完成一次对话和工具调用，再逐步验证 Memory 与 Skills，为第二周第三题的对比分析积累实际依据。

本轮使用本机环境。新软件安装最初计划由用户操作，后经用户授权，目录配置和安装已由 AI 执行；模型配置和学习实验仍按后续步骤开展。用户后来单独授权 AI 执行旧 Hermes 的完全卸载和数据删除，该环节已由 AI 完成。实际 API Key 由用户在本机填写，不写入学习笔记或 Git 仓库。

| 阶段 | 验收依据 | 当前状态 |
|---|---|---|
| 环境检查 | 系统架构、系统版本、基础工具和磁盘信息 | 已完成，结果见第三节 |
| 旧安装清理 | 旧程序、数据、命令和自启动服务均移除 | 已完成，见第五节 |
| 新安装 CLI | 安装结果、命令路径、版本输出及 PM 检查 | 已完成，见第七节 |
| 配置 DeepSeek | 实际 Provider、模型选择与连接结果 | 已保存 DeepSeek / deepseek-flash / medium；首次对话通过 |
| 首次对话与工具调用 | 多轮回复及真实文件操作结果 | 已通过文本对话、文件写入及读取验证 |
| Memory 与 Skills 实验 | 跨会话读取、方法保存与复用结果 | Memory 已通过；Skill 创建、加载及新任务复用已通过 |

## 二、已核对的安装信息

2026-10-28 核对官方文档：macOS 支持命令行安装；源码安装脚本会准备所需运行环境和依赖。首次安装默认跟随稳定版本。官方 Provider 列表包含 `deepseek`，其凭证环境变量为 `DEEPSEEK_API_KEY`。

官方默认源码目录为 `~/.hermes/hermes-agent/`，CLI 入口为 `~/.local/bin/hermes`，默认用户数据目录为 `~/.hermes/`。源码目录可以通过安装脚本的 `--dir` 指定，用户数据目录由 `HERMES_HOME` 控制。具体安装路径将在执行安装前确定，结合此前将软件和数据放在 `Work-Data` 的习惯安排；本步骤不创建或迁移目录。

参考：[官方安装指南](https://hermes-agent.nousresearch.com/docs/getting-started/installation)、[模型 Provider 配置](https://hermes-agent.nousresearch.com/docs/integrations/providers)。实际安装参数会在对应步骤再次核对。

## 三、第一步：检查本机环境

在 Mac 终端中逐行执行，任意目录均可：

```bash
uname -m
sw_vers -productVersion
git --version
curl --version
xcode-select -p
command -v hermes || echo "当前 PATH 中没有 hermes 命令"
df -h /Volumes/Work-Data
```

| 命令 | 用途 |
|---|---|
| `uname -m` | 确认当前终端使用的 CPU 架构 |
| `sw_vers -productVersion` | 确认 macOS 版本 |
| `git --version` | 检查源码获取工具 |
| `curl --version` | 检查下载工具及其版本 |
| `xcode-select -p` | 查看开发工具目录是否配置，供后续依赖准备参考 |
| `command -v hermes` | 检查当前 PATH 中是否已有 Hermes，避免直接覆盖旧安装 |
| `df -h /Volumes/Work-Data` | 查看计划使用磁盘的挂载及剩余空间 |

如果找不到 `hermes`，表示当前 PATH 没有该命令；仍需结合后续目录检查判断是否存在未加入 PATH 的安装。若某条命令报错，保留错误输出，不先自行安装或修改配置。

### 实际结果（2026-10-28，用户终端反馈）

| 检查项 | 实际输出 | 说明 |
|---|---|---|
| 当前架构 | `arm64` | 当前终端报告 ARM64 架构 |
| macOS | `27.0.1` | 已记录系统版本 |
| Git | `git version 2.48.1` | 命令可执行 |
| curl | `curl 8.7.1 (x86_64-apple-darwin26.0)`；SecureTransport、LibreSSL 3.3.6 | 命令可执行；构建标识与当前终端架构不同，暂不据此判断安装存在故障 |
| 开发工具目录 | `/Applications/Xcode.app/Contents/Developer` | 已配置 Xcode 开发工具目录，尚未执行编译验证 |
| Hermes 命令入口 | `/Users/lifei/.local/bin/hermes` | PATH 已能找到命令，尚不能据此确认版本、来源及运行状态 |
| Work-Data 磁盘 | `/dev/disk5s1`，总计 `500Gi`，已用 `163Gi`，可用 `337Gi`，使用率 `33%` | 已挂载到 `/Volumes/Work-Data`，当前空间充足 |

curl 输出还列出了 HTTP、HTTPS、HTTP2、HTTPS-proxy 等协议或能力；这一步仅确认命令能运行，没有验证安装源和模型 API 的网络连接。

**本步结论：基础环境信息已确认，发现已有 Hermes 命令入口。** 因此下一步先检查已有安装，再决定复用、修复或安装；本轮尚未执行安装或模型配置。

## 四、第二步：核对已有 Hermes 命令

在同一个终端逐行执行：

```bash
ls -l /Users/lifei/.local/bin/hermes
file /Users/lifei/.local/bin/hermes
hermes --version
ls -ld "$HOME/.hermes" "$HOME/.hermes/hermes-agent"
```

- `ls -l`：查看入口文件；如果是软链接，输出会显示它指向的位置。
- `file`：判断入口属于脚本、可执行文件还是软链接。
- `hermes --version`：请求程序输出版本后退出。该选项已在[官方 CLI 命令参考](https://hermes-agent.nousresearch.com/docs/reference/cli-commands)中核对。
- `ls -ld`：查看两个默认目录本身及其软链接关系，不读取其中的配置或凭证。使用自定义安装位置时，默认目录可能不存在。

如果命令报错，保留错误输出，再据此定位；这一步不执行更新、卸载、迁移或重新安装。

### 实际检查与操作方向调整

用户进一步要求将 Hermes 放到 `/Volumes/Work-Data`，并明确表示“旧的可以完全卸载，数据也可以删除”“卸载和删除的过程你来操作”。因此由 AI 检查并清理旧安装，不再等待用户执行上面的整组命令。

实际确认：入口是一个 116 字节的 Bash 脚本，最终执行 `/Users/lifei/.hermes/hermes-agent/venv/bin/hermes`。`~/.hermes` 和其中的 `hermes-agent` 均为实际目录；旧安装及数据共占用约 `618M`（`du -sh` 输出）。未执行版本命令，不记录未经验证的版本号。

同时发现 `ai.hermes.gateway` 自启动服务正在运行，PID 为 `837`；它使用旧安装的 Python，工作目录和日志均位于 `~/.hermes`。

## 五、第三步：完全卸载旧 Hermes 并删除数据

执行日期：2026-10-28。此步骤由用户明确授权 AI 操作，包括删除旧配置、会话、记忆、Skills 和其他旧数据，不做数据迁移备份。

### 1. 确认删除范围

- `/Users/lifei/.hermes`：旧源码、虚拟环境和用户数据。
- `/Users/lifei/.local/bin/hermes`：已确认指向旧源码的命令包装脚本。
- `/Users/lifei/Library/LaunchAgents/ai.hermes.gateway.plist`：已确认属于旧 Hermes 的自启动服务。
- `.zshrc` 与 `.profile` 中各一行 Hermes 安装注释；共用的 `~/.local/bin` PATH 配置保留。

检查没有输出配置文件、API Key 或其他凭证内容。采用[官方文档中的手动卸载方式](https://hermes-agent.nousresearch.com/docs/getting-started/updating)，删除前验证具体路径及其归属。

### 2. 停止自启动服务

实际执行：

```bash
launchctl bootout "gui/$(id -u)" /Users/lifei/Library/LaunchAgents/ai.hermes.gateway.plist
```

命令退出码为 0。随后检查进程列表，已没有旧 Hermes 进程。

### 3. 删除程序和数据

使用 Python 对已核实的精确路径执行删除：先通过 `Path.unlink()` 删除上述 plist 和包装脚本，再通过 `shutil.rmtree()` 删除 `/Users/lifei/.hermes`。删除前断言数据根目录不是软链接、包装脚本指向旧安装、服务标签及启动程序与旧安装一致。

删除成功后，从 `.zshrc` 和 `.profile` 各移除一行 `# Hermes Agent — ensure ~/.local/bin is on PATH` 注释，其他内容保留。没有删除系统 Python、Git、curl 或共用的软件缓存。

### 4. 独立复核

| 检查 | 结果 |
|---|---|
| `~/.hermes` | 已不存在 |
| `~/.local/bin/hermes` | 已不存在 |
| Hermes LaunchAgent plist | 已不存在 |
| `launchctl print gui/<用户ID>/ai.hermes.gateway` | 服务未加载 |
| 进程列表 | 未发现 Hermes 进程 |
| 修改过的 Shell 配置 | 无 Hermes 引用，`zsh -n` 和 `sh -n` 语法检查通过 |
| 新登录 Shell 的命令查询 | 输出 `Hermes command not found` |

**旧 Hermes 完全卸载及数据删除已完成。** 删除的是原先约占 `618M` 的目录；此数值是删除前目录占用，不代表独立测量了磁盘实际释放量。

下一步准备在 Work-Data 安装新版本，继续由用户执行安装命令、AI 指导和记录。源码、用户数据、依赖与缓存的位置需要在安装时一并确认，避免只移动源码却把主要依赖留在内置磁盘。

## 六、第四步：准备 Work-Data 目录和安装脚本

2026-10-28：用户确认程序、数据和缓存均放到 `/Volumes/Work-Data`，新安装仍由用户操作。用户随后授权“这些步骤你来执行吧，记录到文档中就好”。本步已由 AI 执行完成，授权范围为目录准备、脚本下载和语法检查；尚未执行安装。

计划采用以下目录：

| 用途 | 计划路径 |
|---|---|
| 程序源码 | `/Volumes/Work-Data/servers/hermes-agent` |
| 配置、会话、Memory、Skills 与运行状态 | `/Volumes/Work-Data/data/hermes` |
| 下载及依赖缓存 | `/Volumes/Work-Data/cache/hermes` |
| 安装临时文件 | `/Volumes/Work-Data/cache/hermes/tmp` |

官方安装脚本支持 `--dir`、`--hermes-home`，引导运行时支持 `HERMES_RUNTIME_DIR`。下一步再配置安装及日常启动的专用环境，核对运行时、Python/npm 等依赖缓存位置，避免只设置源码目录。不能把改变 `HOME` 当成迁移办法，也不全局修改其他项目的缓存设置。命令入口和 Shell 初始化文件可能仍需在用户目录保留少量文件；macOS 自身日志、交换文件不属于 Hermes 可配置存储。

本步先准备目录、下载脚本并检查语法，**不运行安装脚本**。在同一个终端执行；任意一条报错就停下并反馈：

```bash
df -h /Volumes/Work-Data

mkdir -p /Volumes/Work-Data/servers \
  /Volumes/Work-Data/data/hermes \
  /Volumes/Work-Data/cache/hermes/tmp

chmod 700 /Volumes/Work-Data/data/hermes \
  /Volumes/Work-Data/cache/hermes

curl -fL --connect-timeout 15 --max-time 120 \
  https://hermes-agent.nousresearch.com/install.sh \
  -o /Volumes/Work-Data/cache/hermes/install.sh

bash -n /Volumes/Work-Data/cache/hermes/install.sh && echo "安装脚本语法检查通过"

ls -ld /Volumes/Work-Data/servers \
  /Volumes/Work-Data/data/hermes \
  /Volumes/Work-Data/cache/hermes \
  /Volumes/Work-Data/cache/hermes/tmp
```

`mkdir -p` 创建缺少的目录；程序源码子目录留给安装器创建。`chmod 700` 将 Hermes 专用数据和缓存目录限制为当前用户访问。`curl -o` 只下载文件；`bash -n` 只检查语法，不执行脚本，也不代表安全性或安装验证通过。

依据：[官方安装脚本](https://hermes-agent.nousresearch.com/install.sh)。实际安装将使用本次下载的脚本，执行前再核对路径参数与环境配置。

### 实际执行结果（2026-10-28）

| 检查项 | 实际结果 |
|---|---|
| 磁盘挂载与容量 | `/dev/disk5s1` 挂载于 `/Volumes/Work-Data`；总计 `500Gi`，可用 `336Gi` |
| 目录准备 | `servers`、`data/hermes`、`cache/hermes`、`cache/hermes/tmp` 均已存在 |
| 权限 | `data/hermes` 和 `cache/hermes` 均为 `700`，属主为 `lifei` |
| 脚本下载 | curl 退出码为 0；文件大小为 66,864 字节 |
| 脚本语法 | `bash -n` 通过，输出“安装脚本语法检查通过” |
| 安装执行 | 未运行安装脚本；未配置模型或启动网关 |

下载文件：`/Volumes/Work-Data/cache/hermes/install.sh`。

本地 SHA-256（用于识别本次下载的文件，不代表官方签名验证）：

```text
abf338605dae6dd28d101673ac6aed7995c955745db46d47676bc591a7957209
```

下一步需依据本次下载的实际脚本核对程序、运行时、数据及缓存配置，再执行安装。未提交或推送文档；另一份已有学习笔记保持原样。

## 七、第五步：配置路径并安装（2026-10-28）

用户明确授权 AI 配置并安装。已创建 `/Volumes/Work-Data/data/hermes/environment.sh`（权限 600），仅在 Hermes 安装和启动子进程中载入，避免改变其他项目环境。

| 环境变量 | 路径 |
|---|---|
| HERMES_HOME | `/Volumes/Work-Data/data/hermes` |
| HERMES_RUNTIME_DIR | `/Volumes/Work-Data/servers/hermes-runtime` |
| UV_PYTHON_INSTALL_DIR | `/Volumes/Work-Data/cache/hermes/python` |
| UV_CACHE_DIR | `/Volumes/Work-Data/cache/hermes/uv` |
| PIP_CACHE_DIR | `/Volumes/Work-Data/cache/hermes/pip` |
| npm_config_cache | `/Volumes/Work-Data/cache/hermes/npm` |
| XDG_CACHE_HOME | `/Volumes/Work-Data/cache/hermes/xdg` |
| PLAYWRIGHT_BROWSERS_PATH | `/Volumes/Work-Data/cache/hermes/playwright` |
| TMPDIR | `/Volumes/Work-Data/cache/hermes/tmp/` |

同时创建 `~/.hermes → /Volumes/Work-Data/data/hermes` 软链接，使未显式读取环境变量的默认路径也落到外置盘。内部磁盘只保留路径入口；运行时存储与程序源码分开。未更改 HOME。

实际执行：

```bash
source /Volumes/Work-Data/data/hermes/environment.sh
bash c \
  --dir /Volumes/Work-Data/servers/hermes-agent \
  --hermes-home /Volumes/Work-Data/data/hermes \
  --channel stable --non-interactive \
  > /Volumes/Work-Data/cache/hermes/install-console.log 2>&1
```

`--non-interactive` 跳过模型配置和网关安装交互，不填写凭证。没有启用开机网关。保留官方默认依赖选择，不安装桌面客户端。当前安装正在执行，成功与否待结果验证。源码已检出稳定标签 `v0.22.5`，提交前缀 `bf0203a794`，已进入依赖安装。日志已确认 FFmpeg、Node、npm、Python、ripgrep 安装完成；整体安装仍在执行。

进一步核对已下载源码：PM 会覆写部分通用缓存环境变量，其自有缓存实际在 `/Volumes/Work-Data/data/hermes/cache`，仍在外置盘。另已创建 `data/hermes/tools → /Volumes/Work-Data/servers/hermes-runtime` 软链接，使默认运行时路径与显式配置指向同一份文件，避免命令发布时重复下载。

参考：[官方包管理说明](https://hermes-agent.nousresearch.com/docs/reference/package-management)。

### 安装问题及处理

首次安装在 Python 项目依赖阶段失败，退出码 1。日志显示访问 `https://pypi.tuna.tsinghua.edu.cn/simple/wheel/` 时出现 `tunnel error: unsuccessful`。源码 `pm/index_config.py` 说明 PM 会继承 pip 配置中的镜像地址。

已验证官方 `https://pypi.org/simple/wheel/` 返回 HTTP 200。仅在 Hermes 专用 `environment.sh` 中加入 `UV_INDEX_URL=https://pypi.org/simple` 与 `PIP_INDEX_URL=https://pypi.org/simple`，然后复用相同安装命令重试，日志写入 `install-retry-console.log`。未修改全局 pip 配置或关闭 TLS 校验。重试已通过此前失败的 Python 应用依赖阶段，日志显示 `✓ venv`；浏览器与 cua-driver 均已安装通过，日志显示 `✓ dependencies installed`；配置已生成于外置盘，正在构建命令和界面资源。

### 日常启动入口

已创建外置盘脚本 `/Volumes/Work-Data/data/hermes/bin/hermes`，权限 755，内容如下：

```bash
#!/bin/bash
set -e
source /Volumes/Work-Data/data/hermes/environment.sh
unset PYTHONPATH PYTHONHOME
exec /Volumes/Work-Data/servers/hermes-agent/.hermes/bin/hermes "$@"
```

已在 `~/.zshrc` 末尾加入以下独立 PATH 配置，原有配置保留，`zsh -n` 检查通过：

```bash
# Hermes on Work-Data: the launcher scopes data/cache settings to Hermes.
case ":$PATH:" in
  *":/Volumes/Work-Data/data/hermes/bin:"*) ;;
  *) export PATH="/Volumes/Work-Data/data/hermes/bin:$PATH" ;;
esac
```

官方安装器另会生成 `~/.local/bin/hermes` 小型入口；日常 zsh 优先使用外置盘包装脚本，以保持 Hermes 专用缓存环境。新开终端生效；当前终端可执行 `source ~/.zshrc` 后再使用命令。外置盘必须挂载后才能运行 Hermes。不要将含密钥的 `.env` 放进作业 Git 仓库。

Node 依赖准备、TUI 与 Web 资源构建均通过。安装器还在 `~/.zprofile` 中添加了 `~/.local/bin` 的 PATH 语句，语法检查通过。内部磁盘仅保留小型入口、软链接和 Shell 配置；源码、运行时、虚拟环境、用户数据及已配置的缓存均位于 Work-Data。系统已有 Git、用于引导的已有 Homebrew Python 没有迁移或删除。

### 最终验证结果（2026-10-28）

| 项目 | 结果 |
|---|---|
| 安装重试 | 退出码 0，日志显示 `Hermes Agent install complete` |
| 新登录交互式 zsh | `command -v hermes` 返回 `/Volumes/Work-Data/data/hermes/bin/hermes` |
| `hermes --version` | 退出码 0；Python 3.14.9，安装方式 git，目录为 Work-Data 源码目录 |
| `hermes pm doctor` | 退出码 0；已安装的浏览器、cua-driver、FFmpeg、Node、npm、Python、ripgrep、uv 均通过 |
| 可选工具 | doctor 对 bws、gh、iron-proxy、llamacpp 等列出未安装；未为完成安装额外安装这些可选工具 |
| 用户数据 | `~/.hermes` 实际解析为 `/Volumes/Work-Data/data/hermes` |
| 工具运行时 | `data/hermes/tools` 实际解析为 `/Volumes/Work-Data/servers/hermes-runtime` |
| Python 虚拟环境 | PM facts 指向 `/Volumes/Work-Data/data/hermes/installs/2e14056e6fa2096e/environments/a3168decab6849898f41f8d9d4e03a3d/venv` |
| 凭证文件权限 | 外置盘 `.env` 为 600；没有输出或填写实际凭证 |
| 自启动网关 | `ai.hermes.gateway` 未加载 |

版本显示存在差异，原样记录：安装器选择稳定标签 `v0.22.5`，`git describe --tags --exact-match HEAD` 也为 `v0.22.5`；CLI 输出却是 `Hermes Agent v0.21.5+16231.gbf0203a (2026.9.24) · stable · local bf0203a794a0`。标签与 HEAD 均指向 `bf0203a794a07aac3ff764972ab9087baa4e4f4e`。已确认是同一份安装，未修改上游版本显示代码。

日志保留在外置盘：

- 首次失败：`/Volumes/Work-Data/cache/hermes/install-console.log`
- 成功重试：`/Volumes/Work-Data/cache/hermes/install-retry-console.log`
- 包管理检查：`/Volumes/Work-Data/cache/hermes/pm-doctor.log`

当前终端可执行：

```bash
source ~/.zshrc
hermes --version
```

后续步骤为通过 `hermes model` 配置 DeepSeek，然后分别验证对话与工具调用。本轮仅完成安装；未验证模型连接、真实工具调用、Memory 或 Skills，未提交、推送作业文档。

## 八、第六步：配置 DeepSeek（等待用户操作）

2026-10-28：用户要求由 AI 指导、自己填写 API Key。已核对本机已安装源码的模型配置流程；本步尚未收到实际执行反馈，不记为配置成功。

### 1. 打开模型配置向导

新开终端执行：

```bash
command -v hermes
hermes model
```

命令路径应为 `/Volumes/Work-Data/data/hermes/bin/hermes`。若仍是旧路径，先执行 `source ~/.zshrc`，再检查。

### 2. 选择 Provider 并输入凭证

- 在 Provider 列表选择 **DeepSeek**；若列表分组或分页，以当前屏幕提示进入相应列表。
- 在 API Key 提示处粘贴自己从 DeepSeek 平台取得的有效密钥，按回车。密钥只在本机交互提示中填写，不作为 shell 命令参数，不发到聊天或写进作业文档。
- 如果提示已有密钥，按界面选择保留或替换；不复制含密钥片段的输出。
- 遇到 `Base URL [https://api.deepseek.com/v1]:` 时直接回车保留默认值。若默认值不同，使用 `https://api.deepseek.com/v1`。
- 到模型选择界面先停下，反馈可选模型名称，再决定本次实验使用哪个模型；不预设已过时的模型 ID。

依据为本机 `hermes_cli/auth.py` 的 DeepSeek 注册项及 `model_setup_flows.py` 的 API Key Provider 流程。凭证变量名是 `DEEPSEEK_API_KEY`，端点变量名是 `DEEPSEEK_BASE_URL`。

### 3. 验证计划与当前状态

| 验证层次 | 通过依据 | 当前状态 |
|---|---|---|
| 凭证输入 | 本机向导接受输入，不展示密钥 | 待反馈 |
| 模型选择与保存 | 确认 Provider、模型名称及保存结果 | 待操作 |
| 模型连通性 | 真实对话收到模型回复 | 待验证 |
| 工具调用 | 指定外置盘练习目录中产生预期文件 | 待验证 |

模型列表展示、配置保存和真实对话成功是不同结果，分别记录。后续只记录模型名称、脱敏错误及实际回复，不收集密钥或完整凭证文件。

### 用户反馈：已到模型选择界面

用户截图显示 `Select default model`，可选项为：

- `deepseek-flash`（当前选中）
- `deepseek-v4-pro`
- Enter custom model name
- Skip (keep current)

下一步先选择当前的 `deepseek-flash`，按 Enter，反馈保存结果或下一屏提示。此处仅选择首次实验模型，不据模型名称推断价格、速度或能力。到达模型列表不等于密钥已验证成功，也不代表配置已保存。

保存完成后计划运行 `hermes`，输入“请只回复：DeepSeek 连接成功。不要调用任何工具。”进行首次真实对话验证。当前尚未收到保存或对话结果，均保持待验证。

### 用户反馈：推理强度选择

选择模型后，向导继续显示 `Select reasoning effort`。截图中 `medium` 标记为 `currently in use`，其余选项包括 minimal、low、high、xhigh、max、ultra、Disable reasoning、Skip (keep current)。

本次保持 `medium`，按 Enter 继续。Reasoning effort 表示向模型请求的推理强度；这是 Hermes 的统一配置界面，选项如何生效取决于 Provider 和模型支持，不能仅凭菜单认定 DeepSeek 支持所有档位。首次连通性实验先沿用当前值。

此时仍处于配置向导内，尚未确认配置保存或模型连通。待向导结束返回终端，再执行前述 `hermes` 对话验证；若出现下一项设置，继续按实际界面指导并记录。

### 用户反馈：使用统计分享提示

启动界面显示 `Help improve Hermes?`，询问是否向 Nous 分享使用统计。依据用户截图中的说明，统计包括活动、结果、错误类别、模型路由、Token 总量、功能使用和粗粒度机器信息；界面声明不包含提示词、文件、路径、设置值或错误原文。选择分享后每天发送一次；除一次安装记录外，不发送用户同意前的数据。以上为界面声明，未独立审计其实现。

- `Share with Nous`：同意分享统计。
- `No thanks`：拒绝分享统计。

截图当前选中 `No thanks`。本次学习可保持此项，按 Enter 继续；这是统计分享选择，不是 DeepSeek 密钥或模型连接测试。界面还说明可通过 `hermes setup telemetry` 随时修改；再次询问的原因是早期版本可能在用户看到提示前就保存了拒绝选项。

当前仅记录提示与指导，尚未收到用户最终选择结果或模型回复。

### 用户反馈：配置保存完成，首次对话尚未返回

最新截图明确显示：

- `Default model set to: deepseek-flash (via DeepSeek)`：默认模型已设为 DeepSeek 的 deepseek-flash。
- `Reasoning effort set to: medium`：推理强度已保存为 medium。
- `Shared metrics off`：已关闭使用统计分享。

截图顶部的 `Current model: anthropic/claude-opus-4.6` 是本次修改前的状态输出；后面的保存提示显示新模型已设定。`Found 2 model(s) from DeepSeek catalog` 表示找到两个模型选项，不单独作为真实对话成功依据。截图包含掩码密钥提示，本文不抄录密钥片段，也不保存该截图到仓库。

用户已运行 `hermes`，底部出现测试输入“请只回复：DeepSeek 连接成功。不要调用任何工具。”，但截图尚无模型回复或明确的输入框状态。无法仅凭截图判断消息是否已正式提交、程序是否仍在启动或等待请求。先等待约 30 秒；若仍没有正常对话界面或回复，反馈当前画面及是否已按 Enter，再排查，不重复粘贴请求。

当前结果：配置保存已确认；真实对话连通性仍待验证。

### 首次真实对话验证通过

2026-10-28 用户提供新的运行截图：

- 用户输入：“请只回复：DeepSeek 连接成功。不要调用任何工具。”
- Hermes 实际回复：“DeepSeek 连接成功。”
- 底部状态栏显示 `deepseek-flash`。
- 本轮界面显示耗时 `23.7s`，仅记录此次观察，不代表模型固定响应速度。

据此确认当前 Hermes + DeepSeek 的首次文本对话流程已跑通。截图未展示工具调用，尚不能作为工具能力验证。输入栏里的 `Explain this error and how to fix it` 是待输入区域的示例提示，不是本轮错误信息。

## 九、第七步：文件工具实验（已验证）

继续在当前 Hermes 会话输入：

```text
请完成一次文件工具实验：
1. 检查 /Volumes/Work-Data/data/hermes/homework-lab 目录是否存在；不存在就创建。
2. 在该目录新建 hello-hermes.txt，内容为：这是我使用 Hermes 和 DeepSeek 完成的第一次文件工具调用。
3. 如果文件已经存在，先读取并报告，不覆盖。
4. 创建成功后重新读取文件，向我报告完整路径和实际读到的内容。
只操作这个目录，不修改其他文件。
```

若弹出工具执行确认，核对操作路径仅涉及上述目录再允许。完成后反馈工具执行记录和最终回复，不仅依据模型声称成功就判定文件已创建。可在另一个终端独立复核：

```bash
cat /Volumes/Work-Data/data/hermes/homework-lab/hello-hermes.txt
```

本步尚未执行或验收；目标是观察模型决定调用工具、Hermes 执行、结果返回给模型的实际循环。所有实验文件位于 Work-Data。

### 实际结果：文件工具实验通过

2026-10-28，用户截图显示 `Write` 写入目标文件、`Read` 读取文件及最终结果。界面统计为 `edited 1 file · read 1 file · ran 1 command`，本轮显示耗时 55.8 秒。Hermes 回复称目录原本不存在，已用 `mkdir -p` 创建，并在写入前确认文件不存在。

AI 随后进行独立只读检查，未创建或修改实验文件：

- 实际路径：`/Volumes/Work-Data/data/hermes/homework-lab/hello-hermes.txt`。
- 文件大小：76 字节。
- 实际内容：“这是我使用 Hermes 和 DeepSeek 完成的第一次文件工具调用。”，末尾有换行。
- 内容与要求一致，文件写入和读取实验通过。

证据边界：截图支持本轮发生了写入、读取和命令执行，文件检查证明目标产物真实存在；没有对整个磁盘进行前后快照比较，因此“其他文件完全未改动”仅为 Hermes 的回复，不作为独立核实结论。目录创建步骤的细节来自 Hermes 最终回复，本张截图没有展开该命令的完整调用。

本步学习重点：模型生成工具调用请求，Hermes 执行本地操作，并将结果反馈给模型，最后由模型汇总。再次读取与独立文件核对，比仅相信“实验完成”的自然语言回复更有说服力。本步还不证明跨会话 Memory 或可复用 Skills 已生效。

### 实验复盘：是否每次写入都需要再次读取

用户向 Hermes 追问为何写入后再次读取，并提供了回答截图。核心结论是：操作成功与结果符合需求需要区分，但验证方式应依据工具已经提供的保证，避免无意义重复。

核对本机源码：`tools/file_tools.py` 的 write_file 描述明确说明 `verified: true` 表示写入后的内容哈希已确认，并建议不要仅为确认写入而重复读取；`tools/file_operations.py` 的 `_verify_written_hash` 会比较文件的 SHA-256 与预期字节。本张截图未展示原始工具返回，因此只能确认该字段的设计含义，不能独立确认上一轮确实返回了该字段。

对 Hermes 回答的修正：

- 已核实文件为 76 字节，但“30 来个汉字乘以 3”不是严谨的计算；文件还含英文、空格及换行。
- 写入成功、文件可回读与断电后仍持久保存是不同保证；普通回读可能读取操作系统缓存，不能据此断言物理磁盘持久化。
- 磁盘满、编码错误、路径问题是可能的风险类别，不是本次实验已经发生的问题。
- 内容哈希一致证明写入结果与提交字节一致，仍不等于这些内容满足用户的语义需求。

本次明确要求重新读取，是为了学习完整的工具反馈过程；AI 在 Hermes 之外读取产物，则提供了独立验收证据。日常操作若可信工具已经验证内容一致，可以利用其返回结果，不必机械地每次读回。

## 十、第八步：跨会话 Memory 实验（已通过）

目标：验证偏好能否通过长期记忆保存，并在新的会话中使用，区别于当前对话上下文。已核对本机源码：`tools/memory_tool.py` 提供 memory 工具，支持用户偏好目标 `user`；命令注册表定义 `/new` 为新建会话 ID 和历史。仅在当前会话复述内容不足以证明跨会话记忆。

### 1. 在当前会话保存一条真实约定

在 Hermes 中输入：

```text
请使用 memory 工具，把下面这条长期约定保存到用户偏好（target=user）：
我的 Hermes 学习实验文件统一放在 /Volumes/Work-Data/data/hermes/homework-lab，除非我明确指定其他目录。
如果已有相同约定，不要重复添加。不要修改其他记忆，也不要创建实验文件。完成后报告工具是否保存成功。
```

这条约定沿用已使用的实验目录，不写入密钥或虚构个人资料。如果出现记忆写入确认，核对只包含上述约定再允许。先反馈 Memory 工具调用及保存结果，不仅凭“我记住了”判断成功。AI 本轮不会代替 Hermes 写入记忆。

### 2. 保存确认后的跨会话验证计划

待第一步确认后，输入 `/new` 新建会话，再询问“我的 Hermes 学习实验文件应该放在哪里？请只回答目录，不搜索历史会话、不读取文件，也不要创建文件。”问题中不重述目标路径。

预期为返回 `/Volumes/Work-Data/data/hermes/homework-lab`。结合保存工具结果、新会话建立和正确回答判断实验；若使用了历史检索或文件读取，需要另行区分，不能直接判定由 Memory 注入得出。后续按实际结果记录，当前两步均未验证。

### 用户反馈：Memory 写入步骤完成

2026-10-28，用户截图显示 `Memory +user` 调用，写入内容为实验目录约定，底部统计 `updated 1 memory`。Hermes 最终回复报告 `success: true`、`entry_added`、`target=user`，并报告当前 user 记忆为 1 条、79/1375 字符。这些字段与计数来自截图中的最终回复，未展开检查原始工具返回或记忆文件。

当前证据支持已执行 Memory 写入；跨会话取用仍待验证。下一步在 Hermes 输入 `/new`，确认进入新会话后，单独发送不包含答案的问题：

```text
我的 Hermes 学习实验文件应该放在哪里？请只回答目录，不搜索历史会话、不读取文件，也不要创建文件。
```

反馈新会话提示与回复。正确答案应为此前保存的实验目录；需结合新会话和工具记录判断，不能把当前会话内的复述当作长期记忆验收。截图中关于记忆注入频率的解释是 Hermes 的自述，本轮不据此确认具体实现机制。

### `/new` 的确认提示

用户执行 `/new` 后出现确认框，说明会开启新会话并丢弃当前对话历史。当前尚未确认，新会话尚未建立。

选择 `[1] Approve Once`（仅本次允许）：输入 1 后回车，或保持第一项选中并回车。`[2] Always Approve` 会永久跳过该确认，本实验不需要改变此偏好；`[3] Cancel` 保留当前对话。若等待超时而取消，可重新输入 `/new`。

本步骤用于去除当前对话上下文，从而验证长期记忆；不把提示中的“丢弃历史”扩大解释为删除所有磁盘会话记录。确认完成后，再发送上面的目录提问。跨会话实验仍待实际回复验收。

### 最终结果：跨会话 Memory 验证通过

2026-10-28，用户截图提供了以下连续证据：

1. 第一次 `/new` 显示 `cancelled (no input)`，表示未输入确认而取消，并非 Memory 失败。
2. 再次执行 `/new` 后显示 `New session started!`，新会话已建立。
3. 用户询问实验文件目录，问题没有重述目标路径，并明确要求不搜索历史、不读取文件、不创建文件。
4. Hermes 返回 `/Volumes/Work-Data/data/hermes/homework-lab`，与此前保存的 user 记忆一致；截图没有展示工具调用。本轮界面耗时为 11.1 秒，仅作为本次观察。

结合此前 `Memory +user` 写入记录，新会话正确回答的结果支持本次跨会话 Memory 实验通过。无需在新会话再次告知目录。证据来自用户操作截图，没有额外读取完整会话日志或记忆文件，也不将其扩大为所有记忆行为或进程重启后的验证。

学习结论：当前会话历史与长期记忆是不同层次。新会话不再沿用原聊天上下文，但保存的用户约定仍可用于回答。这不是 DeepSeek 模型权重被训练，而是 Hermes 管理持久记忆并向模型提供相关信息。下一项为 Skills 方法保存和复用实验，尚未开展。

## 十一、第九步：Skills 创建与复用实验（已验证）

目标：将文件实验的方法保存为可复用 Skill，再在新会话用新的文件名和内容执行。Memory 保存用户约定，Skill 保存完成某类任务的步骤与约束；创建成功与实际复用成功分别验收。

已核对当前源码 `tools/skill_manager_tool.py`：支持创建含 YAML frontmatter 的 `SKILL.md`，默认保存于 Hermes skills 目录，也支持 `skills.create_dir` 配置。具体落盘路径以本次工具结果为准。由用户在 Hermes 中操作，AI 不代替 Hermes 创建技能。

### 1. 创建文件实验 Skill

在当前 Hermes 会话输入：

```text
请把“安全创建并核验学习实验文本文件”的方法保存为一个 Skill，名称为 homework-file-check，使用 Hermes 提供的技能管理工具创建。

Skill 的适用场景：我要求在学习实验目录中创建一个文本文件并核验结果。
步骤与约束：
1. 确定实验目录：优先采用我本次明确指定的目录，否则使用已保存的用户约定；两者都没有就询问，不猜测。
2. 根据本次任务取得文件名和文本内容；缺失就询问，不固定成 hello-hermes.txt 或旧实验文案。
3. 检查目录和目标文件；目录不存在时创建，文件已存在则停止写入并报告，不覆盖。
4. 使用文件工具写入，然后读取文件，核对实际内容是否与本次要求一致。这一步回读是本教学实验的明确验收要求。
5. 报告完整路径、内容及核验结果，只操作指定实验目录。

只创建这个 Skill，不执行文件实验，不修改其他 Skill 或 Memory。如果同名 Skill 已存在，先报告，不覆盖。完成后告诉我实际保存路径和创建结果。
```

若出现技能写入确认，核对名称及范围后允许。本步骤仅保存方法；允许写入该 Skill 所在目录，“只操作指定实验目录”是 Skill 日后执行文件实验时的约束。

### 2. 后续复用计划

先检查创建工具记录、实际 `SKILL.md` 路径及正文，再用 `/new` 新会话明确要求调用 `homework-file-check`，传入不同的文件名和内容。观察是否加载技能、执行写入与读取，并独立检查实际文件。当前尚未创建或复用，不将本节计划记为已完成。

### 创建结果与独立检查

2026-10-28，用户截图显示 Hermes 报告创建 `homework-file-check`，并在首次描述长度校验失败后缩短描述、补全元数据，再读回检查。截图底部显示 `updated 2 skills`；该统计不能单独证明创建了两个不同 Skill，本轮未检查完整调用日志。

AI 随后只读检查实际 `SKILL.md`，确认文件存在，包含 name、description、适用场景、目录优先级、按任务获取文件名与内容、存在性检查、写入及回读、结果报告等步骤。未修改技能正文。

截图显示的 `~/.hermes/skills/homework-file-check/SKILL.md` 经软链接解析为：

`/Volumes/Work-Data/data/hermes/skills/homework-file-check/SKILL.md`

因此 Skill 仍保存在外置盘。正文把当前实验目录作为 Memory 约定的示例写入；若以后更改目录，应避免把这一示例当作新的 Memory 验证证据。创建已确认，复用尚未验证。

### 下一步：在新会话复用

执行 `/new` 并选择仅本次允许，看到 `New session started!` 后发送：

```text
请加载并按照 homework-file-check Skill 完成一次新实验。
文件名：skill-reuse-01.txt
内容：这是通过复用 homework-file-check Skill 完成的新任务。
目录按 Skill 的规则确定。若文件已经存在，请停止，不覆盖。
请实际执行并报告核验结果，不要修改 Skill 或 Memory。
```

本次不重复提供详细操作步骤，观察 Hermes 是否加载技能后完成检查、写入、回读。反馈技能加载记录、工具执行记录和最终回复，再独立核查产物。当前仅提供操作指导，未代用户执行实验。

### 最终结果：Skill 加载与新任务复用通过

2026-10-28，用户截图显示以下执行顺序：加载 `Skill homework-file-check` → Shell 预检查 → `Write` 新文件 → `Read` 回读 → Shell 附加核验 → 汇总结果。底部统计为 `edited 1 file · read 1 skill, 1 file · ran 2 commands`，本轮耗时 53.4 秒。这证明本轮实际加载了技能，而不是仅声称遵循技能。

AI 独立只读检查产物：

- 路径：`/Volumes/Work-Data/data/hermes/homework-lab/skill-reuse-01.txt`。
- 正文：“这是通过复用 homework-file-check Skill 完成的新任务。”
- 正文 UTF-8 为 66 字节，加一个结尾换行，总计 67 字节。
- 内容与本次任务要求一致，Skill 加载与新文件任务复用通过。

截图中 Hermes 报告“正文 60 字节 + 换行 1 字节”，与其总计 67 字节自相矛盾；应以上述实际检查的 66 + 1 为准。文件内容核验通过，不代表生成的所有解释都准确。

本截图没有包含 `/new` 成功提示，因此本轮确认 Skill 加载和复用，不单靠这张截图断言新会话建立已独立核实。目录和目标文件的写入前状态、未修改其他 Skill 或 Memory 的说法来自 Hermes 报告；本轮未做完整前后快照审计。

本次显式指定了技能名称，验证的是“显式调用后复用”，并未验证模型能否在不点名技能时自动选择。下一步可将已完成的本地安装、真实对话、文件工具、跨会话 Memory 和 Skill 复用证据用于第二周第三题的对比总结；不能据此宣称所有 Hermes 功能均已验证。

## 十二、后续记录方式

收到每一步输出后，及时补充实际结果、遇到的问题和处理方法，再给出下一步操作。安装完成、模型连接成功、工具调用成功和 Memory/Skill 验证分别记录，不能互相替代。

相关内容：[Hermes Agent 对话学习笔记](./2026-10-28-第三题-HermesAgent对话学习笔记.md)、[第二周作业](../02-第2周作业.md)。
