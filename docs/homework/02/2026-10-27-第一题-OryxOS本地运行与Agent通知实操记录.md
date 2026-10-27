[toc]

# 第二周第一题：OryxOS 本地运行与 Agent 通知实操记录

> 记录日期：2026-10-27。
>
> 阅读方式：按步骤对照命令、配置与实际结果。“已完成”表示已有本次执行证据；文中的创建和触发命令用于复习，现有配置已生效，不需要重复执行。
>
> 当前进度：第一题已完成。OryxOS 已在本机启动，`homework-hello` Agent 已通过 DeepSeek 执行一次任务，`notify` 成功调用一次；用户已确认飞书群收到指定消息。配置方法、命令、执行记录及验收证据见下文。

## 一、这道题要完成什么

题目来源：[第二周作业](../02-第2周作业.md)。

要求是：克隆官方 OryxOS，运行起来，配置第一个 Agent，执行并发出通知。

本次采用的链路：

`OryxOS → DeepSeek 模型 → notify 工具 → 飞书自定义群机器人 → 群内收到消息`。

完成标准分为五层：

| 层次 | 如何证明 | 当前状态 |
|---|---|---|
| 构建 | 生成可执行 JAR，Maven 输出 BUILD SUCCESS | 已完成 |
| 启动 | 进程常驻，健康检查成功，管理台可打开 | 已完成 |
| 模型调用 | Agent 确实调用真实模型并获得结果 | 已完成，2 次 LLM 调用成功 |
| 工具执行 | 审计中有成功的 notify 调用 | 已完成，仅调用 1 次 |
| 通知送达 | 指定飞书群实际出现对应消息 | 已完成，用户确认收到 |

构建成功只能证明第一层。服务启动、模型调用和通知送达需要分别验证。

## 二、先分清项目、软件和数据的位置

### 1. 当前项目

本题使用官方仓库：

```text
https://github.com/oryx-labs/oryxos.git
```

本地源码位置：

```text
/Volumes/Work-Data/work/AI/projects/oryxos
```

第一周的 `oryxos-practice` 是另一份练习仓库。本题的操作以这里的官方源码为准。

### 2. 当前软件与配置目录

| 内容 | 实际位置 | 作用 |
|---|---|---|
| SDKMAN 管理入口 | `/Users/lifei/.sdkman` | 保存管理脚本、配置和版本管理入口 |
| SDK 软件存放目录 | `/Volumes/Work-Data/servers/sdkman/candidates` | 存放 SDKMAN 安装的软件 |
| JDK | `/Volumes/Work-Data/servers/sdkman/candidates/java/21.0.12+1.1-tem` | 编译、运行 Java |
| Maven | `/Volumes/Work-Data/servers/sdkman/candidates/maven/3.9.16` | 解析依赖、构建项目 |
| Maven 本地仓库 | `/Volumes/Work-Data/data/maven-repository` | 保存下载的依赖和 Maven 插件 |
| Maven 用户配置 | `/Users/lifei/.m2/settings.xml` | 指定本地仓库等用户级设置 |
| Shell 配置 | `/Users/lifei/.zshrc` | 新终端加载 PATH 和 SDKMAN |

Maven 软件目录和 Maven 本地仓库是两回事。切换 Maven 版本，可以继续使用同一个本地仓库。

当前仓库配置已经符合要求，因此保留：

```xml
<localRepository>/Volumes/Work-Data/data/maven-repository</localRepository>
```

用户级 `~/.m2/settings.xml` 优先于 Maven 安装目录下的同名设置。本次没有搬动依赖缓存，也没有修改仓库路径。

### 3. 项目运行文件

以下路径均相对项目根目录：

| 文件或目录 | 作用 | 当前状态 |
|---|---|---|
| `.sdkmanrc` | 项目要求的 Java、Maven 版本 | 已存在 |
| `config/application.yml` | 本机 OryxOS 运行配置 | 已准备 |
| `.env` | 本机凭证，通过环境变量提供给进程 | DeepSeek、飞书 Webhook 及签名密钥均已填写 |
| `.oryxos/` | Agent、Skill、记忆等运行工作区 | 已初始化 |
| `oryxos.db` | 会话、审计、Provider、通知渠道等运行数据 | 已初始化 |
| `.oryxos/master.key` | 本地主密钥，用于保护落库凭证 | 首次启动检查时已生成 |
| `oryxos-boot/target/oryxos-boot-0.1.8-RELEASE.jar` | 可执行运行包 | 已生成 |

`.env`、本机配置、数据库和工作区均已被 Git 忽略。数据库与 `master.key` 要配套保管，不能为了重试启动随意删除。

## 三、步骤 1：确认源码来源与版本（已完成）

进入项目目录：

```bash
cd "/Volumes/Work-Data/work/AI/projects/oryxos"

git remote -v
git status -sb
git log -1 --oneline
```

这些命令分别回答：

- 代码来自哪个仓库。
- 当前分支、未提交改动及与远端的关系。
- 当前运行依据是哪一次提交。

本次检查时，工作区干净，但本地 `main` 比已获取的 `origin/main` 落后 261 个提交。确认远端后进行了快进同步：

```bash
git merge --ff-only origin/main
```

`--ff-only` 只允许快进：分支发生分叉时直接失败，不自动生成合并提交。

本次记录对应：

```text
分支：main
提交：80338915127c1e0ce27319dc68e2f826b368b515
项目版本：0.1.8-RELEASE
```

以后复现时先记录自己实际的提交。上游继续演进后，文件、界面和依赖版本可能变化。

## 四、步骤 2：用 SDKMAN 安装项目工具链（已完成）

### 1. SDKMAN 管什么

SDKMAN 是版本管理工具，负责安装和切换 JDK、Maven 等软件。真正编译项目的是 Maven 和它调用的 JDK。

本项目的 `.sdkmanrc`：

```properties
java=21.0.12+1.1-tem
maven=3.9.16
```

加载 SDKMAN 并安装项目要求的版本：

```bash
source "/Users/lifei/.sdkman/bin/sdkman-init.sh"

cd "/Volumes/Work-Data/work/AI/projects/oryxos"

sdk env install
sdk env
```

- `sdk env install`：安装 `.sdkmanrc` 指定、但本机尚未安装的版本。
- `sdk env`：在当前终端使用项目指定版本。

### 2. 本次遇到的联网报错

曾出现：

```text
INTERNET NOT REACHABLE
Stop! java 21.0.12+1.1-tem is not available. Internet unreachable.
Stop! java 21.0.12+1.1-tem is not installed.
```

这是一组连带错误：联网检查没有通过，SDKMAN 无法查询或下载版本，安装也没有完成。

不能仅凭这段输出判断“Java 版本不存在”，也不能据此认定整台电脑都无法上网。

检查终端是否能访问 SDKMAN 服务：

```bash
curl -fsSL --connect-timeout 10 --max-time 20 \
  https://api.sdkman.io/2/healthcheck
```

浏览器与终端可能使用不同的代理配置。本次网络配置由用户自行处理，随后安装成功。

提示中的 `sdkman_healthcheck_enable=false` 只会跳过健康检查，不会修复真实下载链路；不能把关闭检查当成网络修复的证明。

## 五、步骤 3：迁移 JDK、Maven 的安装目录（已完成）

### 1. 迁移方案

软件放到 `/Volumes/Work-Data/servers`，Maven 依赖仓库保持原位置。

SDKMAN 入口保持不变，通过软链接访问实际软件：

```text
/Users/lifei/.sdkman/candidates
    → /Volumes/Work-Data/servers/sdkman/candidates
        ├── java/
        │   ├── 21.0.12+1.1-tem/
        │   └── current → 21.0.12+1.1-tem
        └── maven/
            ├── 3.9.16/
            └── current → 3.9.16
```

这样继续使用 `sdk install`、`sdk use` 等命令时，软件实际存放在 Work-Data 卷。

### 2. 本次迁移过程

以下是已经执行过的记录，**当前不要再次直接执行这些迁移命令**。

先备份 `.zshrc`，并用 `ditto` 复制软件目录：

```bash
cp -p "/Users/lifei/.zshrc" \
  "/Users/lifei/.sdkman/migration-backup-20261027.m8fPt6/zshrc"

mkdir -p "/Volumes/Work-Data/servers/sdkman"

ditto "/Users/lifei/.sdkman/candidates" \
  "/Volumes/Work-Data/servers/sdkman/candidates"
```

然后进行内容校验：

```bash
rsync -acni "/Users/lifei/.sdkman/candidates/" \
  "/Volumes/Work-Data/servers/sdkman/candidates/"
```

其中 `-a` 保留目录与链接等属性，`-c` 校验文件内容，`-n` 只检查、不写入，`-i` 显示差异。本次未报告差异。

确认新位置的 JDK 可运行后，把原安装目录保留为备份，再建立兼容入口：

```bash
mv "/Users/lifei/.sdkman/candidates" \
  "/Users/lifei/.sdkman/migration-backup-20261027.m8fPt6/candidates"

ln -s "/Volumes/Work-Data/servers/sdkman/candidates" \
  "/Users/lifei/.sdkman/candidates"
```

本次原目录是正常目录，目标位置事先为空；这些前提是迁移操作成立的条件。

备份仍保留在：

```text
/Users/lifei/.sdkman/migration-backup-20261027.m8fPt6
```

里面包含原安装目录和迁移前的 `.zshrc`。保留备份意味着它仍占用原盘空间，本次没有删除。

### 3. 修正终端加载顺序

原来 zsh 的 SDKMAN 初始化被注释，且手动指定了旧 Maven。

本次将 `M2_HOME` 改为 SDKMAN 的 Maven 入口，并把 SDKMAN 初始化放到 `.zshrc` 末尾：

```bash
export M2_HOME="$HOME/.sdkman/candidates/maven/current"

# SDKMAN 初始化位于其他 PATH 配置之后。
export SDKMAN_DIR="$HOME/.sdkman"
[[ -s "$SDKMAN_DIR/bin/sdkman-init.sh" ]] && source "$SDKMAN_DIR/bin/sdkman-init.sh"
```

这解决了“软件已安装，但新终端仍优先使用旧路径”的问题。其他原有配置保留，网络配置未修改。

已有终端需要重新加载，或者打开一个新终端：

```bash
source "/Users/lifei/.zshrc"
```

## 六、步骤 4：验证 Maven 真正在使用哪个 JDK（已完成）

在项目目录执行：

```bash
sdk env
sdk current

command -v java
java -version

command -v mvn
mvn -version
```

本次实际验证：

```text
SDKMAN Java 标识：21.0.12+1.1-tem
java -version：21.0.12.1，Eclipse Temurin
Maven：3.9.16
Maven 使用的 Java：21.0.12.1，Eclipse Adoptium
Java 实际运行目录：
/Volumes/Work-Data/servers/sdkman/candidates/java/21.0.12+1.1-tem
```

SDKMAN 的版本标识和 `java -version` 的显示格式不同，这里对应的是同一份 JDK。

不能只检查 `java -version`。Maven 可能受 `JAVA_HOME` 影响而调用另一份 JDK，所以必须同时检查 `mvn -version`。

本次还在新登录终端中、不手动运行 `sdk env` 的情况下验证了自动加载，并通过独立的离线 Maven 配置验证，确认：

```text
Using local repository at /Volumes/Work-Data/data/maven-repository
BUILD SUCCESS
```

日常也可查询有效仓库路径；第一次使用可能需要下载 Maven Help 插件：

```bash
mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout
```

## 七、步骤 5：构建 OryxOS（已完成）

### 1. 常规构建

```bash
cd "/Volumes/Work-Data/work/AI/projects/oryxos"
sdk env

mvn -B -ntp package -DskipTests
```

| 参数 | 作用 |
|---|---|
| `-B` | 批处理输出 |
| `-ntp` | 不显示依赖下载进度条 |
| `package` | 编译并打包 |
| `-DskipTests` | 跳过 Java 单元测试执行；通常仍编译测试代码 |

构建同时会打包 Web 管理台，最终产物是包含后端与管理台的运行包。

### 2. 本次下载缓慢的处理

最初的构建反复通过 Spring Milestones、阿里云镜像重新核验已有依赖。

为本次命令准备了临时的 Maven Central 镜像设置，成功执行的命令是：

```bash
mvn -B -ntp \
  -gs /tmp/oryxos-homework-build.qoWjfl/settings.xml \
  package -DskipTests
```

`-gs` 指定这一次构建使用的全局设置文件，用户级 `~/.m2/settings.xml` 仍会读取，因此本地仓库路径继续生效。

临时文件中的关键配置为：

```xml
<mirrors>
  <mirror>
    <id>direct-central</id>
    <url>https://repo.maven.apache.org/maven2</url>
    <mirrorOf>*</mirrorOf>
  </mirror>
</mirrors>
```

这个 `/tmp` 路径是本次执行记录，可能被系统清理；以后不能假定它永久存在。它只服务于此次正式发行版依赖的下载，没有修改项目 POM 或本机永久镜像配置。

### 3. 实际结果及证据边界

```text
OryxOS 0.1.8-RELEASE
31 个 Maven 模块：全部 SUCCESS
BUILD SUCCESS
Total time: 17:32 min
```

产物：

```text
/Volumes/Work-Data/work/AI/projects/oryxos/oryxos-boot/target/oryxos-boot-0.1.8-RELEASE.jar
```

实际顺序是：先使用原有 Java 21.0.8、Maven 3.9.11 完成构建，之后再安装并迁移 SDKMAN 工具链。

切换 SDKMAN 后已经验证工具版本和 Maven 配置，但尚未用新工具链重新执行整份 OryxOS 构建。本次 Java 单元测试没有执行，不能写成“全部测试通过”。

当前源码的根 POM 使用 Spring Boot 4.1.2；部分说明仍写 Spring Boot 3.x。运行版本以当前提交的 POM 和实际日志为准。

## 八、步骤 6：初始化工作区并定位启动失败（已完成）

### 1. 初始化工作区

```bash
cd "/Volumes/Work-Data/work/AI/projects/oryxos"

java -jar oryxos-boot/target/oryxos-boot-0.1.8-RELEASE.jar init
```

本次输出：

```text
已初始化工作区 .../.oryxos/（已存在的文件未覆盖）。
```

`init` 创建工作区目录和 Bootstrap 文件。它不等于配置好了真实模型，也不等于已经创建 Agent。

### 2. 本次启动检查

曾执行：

```bash
java -Dserver.address=127.0.0.1 \
  -jar oryxos-boot/target/oryxos-boot-0.1.8-RELEASE.jar \
  serve --port 8080
```

这次检查完成了数据库的 27 个迁移和本地主密钥生成，随后出现：

```text
没有可用的 Provider，请先配置 Provider
```

原因是当时没有可用的真实模型凭证，这次进程已退出。补充凭证后的成功启动结果记录在步骤 8，保留这里作为首次排错记录。

日志开头“运行已启动”只是进入启动流程的提示，必须等健康检查成功才能认为服务就绪。

## 九、步骤 7：填写本机模型凭证（已完成）

### 1. 已准备的运行配置

config/application.yml（本机路径：`/Volumes/Work-Data/work/AI/projects/oryxos/config/application.yml`）：

```yaml
server:
  address: 127.0.0.1

oryxos:
  providers:
    - name: deepseek
      api-key: ${DEEPSEEK_API_KEY:}
      base-url: https://api.deepseek.com
  author:
    provider: deepseek
    model: deepseek-flash
```

- `server.address`：只监听本机。
- `oryxos.providers`：声明准备使用的模型供应商连接。
- `${DEEPSEEK_API_KEY:}`：从环境变量读取 Key，缺失时得到空值。
- `oryxos.author`：用于管理台的模型生成草稿功能；Agent 自身用什么模型还要看它的 `AGENT.md`。

模型选择依据：[DeepSeek 官方模型说明](https://api-docs.deepseek.com/zh-cn/quick_start/pricing/)。

### 2. 填写 .env

打开本机文件：

.env（本机路径：`/Volumes/Work-Data/work/AI/projects/oryxos/.env`）

下面保留空模板示例；真实文件中的三项凭证均已填写，文档不展示它们的值：

```bash
export DEEPSEEK_API_KEY=''
export FEISHU_WEBHOOK_URL=''
export FEISHU_WEBHOOK_SECRET=''
```

填写时，把自己的 DeepSeek API Key 放在第一个变量的单引号内；飞书尚未准备时，后两项保持为空。

密钥在 [DeepSeek 开放平台](https://platform.deepseek.com/api_keys)创建，并确认有可用 API 额度。

后续凭证应直接在本机填写，不写进本学习文档、作业答案、Git 提交或聊天。本次 Key 已配置到权限为 `600` 且被 Git 忽略的文件。

凭证处理提醒：Key 曾出现在聊天记录中，已建议在平台轮换；平台撤销和替换尚未确认。本文不记录其明文。如果轮换已播种进数据库的 Key，还需同步更新 Provider，具体行为见下一节。

本地文件权限已设为仅属主可读写：

```text
-rw-------  .env
```

### 3. 为什么 .env 需要显式加载

当前 `bin/start.sh` 不会自动读取项目根的 `.env`。文件存在，不代表 Java 进程已经拿到变量。

加载命令：

```bash
cd "/Volumes/Work-Data/work/AI/projects/oryxos"
source ./.env
```

模板中的 `export` 让后续启动的 Java 进程继承这些变量。它们只会进入当前终端及其之后启动的子进程。

可以只检查是否填写，不打印真实值：

```bash
test -n "$DEEPSEEK_API_KEY" && printf 'DeepSeek API Key 已设置\n'
```

不要用 `echo "$DEEPSEEK_API_KEY"` 验证，也不要把完整环境变量列表贴到聊天。

## 十、步骤 8：启动服务并验证管理台（已完成）

本次在同一个终端加载版本与凭证后启动：

```bash
cd "/Volumes/Work-Data/work/AI/projects/oryxos"
sdk env
source ./.env

bin/start.sh
```

`bin/start.sh` 会加载 `config/application.yml`、启动运行包并等待健康检查。当前配置将服务限制在本机。

另一个终端进行只读验证：

```bash
curl -fsS http://127.0.0.1:8080/api/v1/health
```

本次独立验证返回的关键字段：

```json
{
  "code": 0,
  "data": {
    "status": "ok"
  }
}
```

启动后首次实际打开 [本机管理台](http://127.0.0.1:8080/admin/)，页面正常显示 OryxOS v0.1.8、一个 `deepseek` Provider，当时 Agent 数量为 `0`。完成步骤 10 后再次检查，Agent 数量为 `1`，详情与执行历史均正常显示。

本次还通过端口检查确认服务监听 `127.0.0.1:8080`。

执行工具中的第一次后台启动曾报告就绪，但命令会话结束后进程没有保留下来。本次改用保活会话运行启动脚本，并再次独立验证健康检查、端口和管理台。自己在普通终端学习时仍可按上面的启动命令操作；不能只凭脚本曾输出“就绪”就判断服务一直在运行。

运行日志：

```text
/Volumes/Work-Data/work/AI/projects/oryxos/logs/oryxos.log
```

本次通过脚本启动的进程，可用 `bin/stop.sh` 停止。不要通过批量终止所有 Java 进程来停服务。

### DeepSeek 连通性验证（已完成）

执行项目已有的连通性检查接口：

```bash
curl -fsS --connect-timeout 3 --max-time 45 \
  -X POST http://127.0.0.1:8080/api/v1/providers/deepseek/test |
jq '{code: .code, data: .data}'
```

本次实际返回：

```json
{
  "code": 0,
  "data": {
    "ok": true,
    "modelCount": 2,
    "sampleModels": [
      "deepseek-flash",
      "deepseek-v4-pro"
    ]
  }
}
```

当前实现会由 OryxOS 服务端携带 Key 请求 DeepSeek 的模型列表，因此可以验证供应商可访问、鉴权成功，以及返回的模型名称。

这项检查本身不是一次对话推理。真实生成、工具调用与飞书送达的实际结果见步骤 10，不能仅凭模型列表检查替代。

### Provider 的一个容易混淆的行为

YAML 中的 Provider 用于首次播种数据库；同名 Provider 已经存在时，启动不会直接覆盖它。

因此，初次配置使用环境变量与 YAML；以后轮换已注册的 Key，应通过管理台或对应 Provider API 更新，不能只改 `.env` 后就假定数据库中的 Key 也变了。

## 十一、步骤 9：配置飞书通知渠道（已完成）

### 1. 准备专用的验收群

在用于本次作业的飞书群中操作：

`群设置 → 群机器人 → 添加机器人 → 自定义机器人`。

保存 Webhook。若启用签名校验，同时保存签名密钥。

操作依据：[飞书自定义机器人官方指南](https://open.feishu.cn/document/client-docs/bot-v3/add-custom-bot?lang=zh-CN)。

把两项凭证填入本机 `.env` 的对应变量，再在执行配置命令的终端 `source ./.env`。

### 2. 当前管理台的限制

本次提交的后端 `FeishuNotifyAdapter` 支持 `config.secret` 加签，但管理台通知表单没有签名密钥字段。

因此，启用签名校验时使用现有 API 传入 `config.secret`。本次没有修改管理台代码，也没有关闭飞书安全校验。

后续在第二题中已针对这一限制及编辑保存丢失密钥的问题完成本地源码修复，见 [第二题修复记录](./2026-10-27-02-第二题-飞书签名密钥修复与PR实操记录.md)。本节保留第一题原运行包的实际操作过程；当前 `8080` 服务尚未替换为修复后的运行包。

### 3. 使用现有 API 注册渠道

本次使用的渠道名为 `homework-feishu`，Agent 引用时使用同一个名字。以下命令说明本次 API 请求的构造方式；渠道已经创建，复习时不要重复注册同名渠道。

前提：服务已启动，两个飞书变量已填好并导出。本机已具备 `curl` 和 `jq`。

```bash
source ./.env

jq -n '{
  name: "homework-feishu",
  type: "feishu",
  url: env.FEISHU_WEBHOOK_URL,
  config: {
    secret: env.FEISHU_WEBHOOK_SECRET
  }
}' |
curl -fsS \
  -X POST http://127.0.0.1:8080/api/v1/notify-channels \
  -H 'Content-Type: application/json' \
  --data-binary @- |
jq '{code: .code, channel: .data.name}'
```

这里由 `jq` 从环境变量构造请求，并通过标准输入交给 `curl`，避免把真实凭证直接写入命令正文。最后只输出结果码与渠道名。

本次注册接口实际返回 `code: 0`；独立查询确认渠道名为 `homework-feishu`、类型为 `feishu`。后端将 Webhook 与签名密钥加密保存到本机数据库。

只查看非敏感字段：

```bash
curl -fsS http://127.0.0.1:8080/api/v1/notify-channels |
jq '{code: .code, channels: [.data[] | {name, type}]}'
```

注册渠道只是保存投递配置，不代表已经向飞书发送消息。

## 十二、步骤 10：创建并运行第一个 Agent（已完成）

### 1. 建立最小 Agent

本次实际配置：

| 配置项 | 实际值 |
|---|---|
| Agent 名 | `homework-hello` |
| Provider | `deepseek` |
| 模型 | `deepseek-flash` |
| 允许的工具 | `notify` |
| 通知渠道 | `homework-feishu` |
| 触发方式 | 手动执行一次 |

本次通过已有的文件保存 API 创建 Agent，没有调用模型生成草稿。接口在校验通过后写入并注册，实际文件为：

.oryxos/agents/homework-hello/AGENT.md（本机路径：`/Volumes/Work-Data/work/AI/projects/oryxos/.oryxos/agents/homework-hello/AGENT.md`）。

文件内容如下：

```markdown
---
name: homework-hello
description: 第二周作业的最小通知 Agent
identity:
  agent_name: homework-hello
  prompt: 你负责执行一次指定的通知任务，并如实报告执行结果。
provider:
  name: deepseek
  model: deepseek-flash
tools:
  - notify
settings:
  max_iterations: 3
  max_history_turns: 5
---

收到手动执行指令时，调用 notify 工具，向 homework-feishu 渠道发送：
“OryxOS 第二周作业：第一个 Agent 已成功运行。”

每次任务最多调用 notify 一次。成功后不要重复发送；失败时报告错误，不要重试，也不要声称通知已经送达。
```

创建过程为：先查询 Agent 列表，确认没有同名 Agent，并取得响应头 `X-Workspace-Revision`；再向 `POST /api/v1/agents/homework-hello/files` 提交文件，使用 `If-Match` 携带该版本，避免覆盖查询之后的工作区变动。

请求体的结构如下，`<完整 Markdown>` 应替换为上面的全文，不能把占位文字直接提交：

```json
{
  "files": {
    "AGENT.md": "<完整 Markdown>"
  },
  "skillBindings": [],
  "knowledgeBindings": []
}
```

首次复现时，可先把上面的 Markdown 存为工作区之外的临时文件 `/tmp/homework-hello-AGENT.md`，再执行下面的创建命令。**当前 Agent 已存在，不需要再次执行。**

```bash
task_workspace_revision=$(
  curl -fsS -D - -o /dev/null http://127.0.0.1:8080/api/v1/agents |
  awk -F ': ' 'tolower($1) == "x-workspace-revision" { sub(/\r$/, "", $2); print $2 }'
)

jq -n --rawfile markdown /tmp/homework-hello-AGENT.md '{
  files: {"AGENT.md": $markdown},
  skillBindings: [],
  knowledgeBindings: []
}' |
curl -fsS \
  -X POST http://127.0.0.1:8080/api/v1/agents/homework-hello/files \
  -H 'Content-Type: application/json' \
  -H "If-Match: $task_workspace_revision" \
  --data-binary @- |
jq '{code: .code, agent: .data | {name, provider, model, tools}}'
```

创建接口返回成功后，另行查询确认 Provider 为 `deepseek`、模型为 `deepseek-flash`、工具仅为 `notify`、没有定时任务。管理台查看路径为“Agent 列表 → homework-hello 详情 → 工作区”；配置总览在“基本信息”中。

这里没有定时任务、额外脚本或其他业务工具。首次练习只验证一次真实的模型与通知链路。

### 2. 执行一次

手动执行内容为：

```text
请执行你的通知任务一次。
```

本次通过手动触发 API 提交一次，等价于让该 Agent 收到上述执行指令：

```bash
curl -fsS \
  -X POST http://127.0.0.1:8080/api/v1/agents/homework-hello/trigger \
  -H 'Content-Type: application/json' \
  --data-binary '{"content":"请执行你的通知任务一次。"}' |
jq '{code: .code, data: .data}'
```

实际返回执行记录 `executionId: 1`、初始状态 `RUNNING`。这是异步启动成功，不代表整轮执行已经结束。随后只查询状态，没有再次触发。**这条命令会真实发送通知，复习时无需重跑。**

```bash
curl -fsS http://127.0.0.1:8080/api/v1/runs/1 |
jq '{code: .code, data: .data | {id, agentName, status, durationMs, errorMessage}}'
```

最终状态为 `SUCCESS`。如果执行失败或触发请求超时，应先检查执行历史，不要连续点击，以免产生重复通知。

### 3. 核对实际送达

需要同时确认：

1. Agent 执行记录成功。
2. 有对应 LLM 调用及 `notify` 工具调用的审计证据。
3. `notify` 使用的渠道和内容正确。
4. 飞书群实际出现对应消息。

本次实际验收结果（北京时间）：

| 项目 | 实际记录 |
|---|---|
| Agent | `homework-hello` |
| 执行记录 | `1`，来源 `manual`，最终状态 `SUCCESS` |
| 开始时间 | 2026-10-27 14:37:54 |
| 结束时间 | 2026-10-27 14:38:10 |
| 总耗时 | 15,892 毫秒，约 15.89 秒 |
| 模型 | `deepseek` / `deepseek-flash` |
| LLM 调用 | 审计记录 `1`、`2`，均为 `success: true` |
| 工具调用 | 审计记录 `1`，工具 `notify`，`success: true` |
| 通知参数 | 渠道 `homework-feishu`，内容与下方固定消息一致 |
| 工具结果 | `已推送`，仅调用 1 次，没有重试 |
| Trace ID | `7714e101-a9ab-45ae-823e-861f3c41c89c` |
| 群内验收 | 用户已明确确认“已收到” |

群内收到的消息：

```text
OryxOS 第二周作业：第一个 Agent 已成功运行。
```

两次 LLM 调用属于同一轮 Agent 执行：第一次生成 `notify` 工具调用，工具执行后，第二次生成最终执行报告。因此“LLM 调用两次”与“通知发送一次”并不矛盾。

运行事件显示的顺序为：

`RUN_STARTED → STEP_STARTED → TOOL_CALL_STARTED → TOOL_CALL_FINISHED → STEP_FINISHED → STEP_STARTED → MESSAGE_CONTENT → STEP_FINISHED → RUN_FINISHED`。

这里可以看到 ReAct 的最小闭环：模型决定调用工具，OryxOS 执行工具并交回结果，模型再报告执行情况。

只读复核命令如下，不会再次发送通知。zsh 中 URL 含 `?` 时应加引号，避免被当作文件名匹配表达式：

```bash
curl -fsS 'http://127.0.0.1:8080/api/v1/audit/llm?limit=20' |
jq '{code: .code, calls: [.data[] | select(.profileName == "homework-hello") | {id, provider, model, success, traceId}]}'

curl -fsS 'http://127.0.0.1:8080/api/v1/audit/tool?limit=20' |
jq '{code: .code, calls: [.data[] | select(.profileName == "homework-hello") | {id, toolName, success, traceId}]}'
```

管理台已实际核对“Agent 列表 → 详情 → 执行历史”，显示本轮“成功”、手动来源、起止时间、15.89 秒及相同 Trace ID。

通知出口会检查 HTTP 状态与飞书业务错误码。本次既有成功的工具记录，也有用户在指定群中的实际收件确认，因此完成了通知送达验收。

可以记录时间、Agent 名、执行记录标识和实际消息文本。截图可作为补充，但要遮挡 Webhook、Key 和签名密钥。

## 十三、当前检查清单

### 已完成

- [x] 确认官方仓库、本地路径和提交版本。
- [x] 安装并验证 SDKMAN 项目所需的 JDK、Maven。
- [x] 把软件迁移到 Work-Data 卷，并保留回退备份。
- [x] 启用 zsh 的 SDKMAN 初始化，验证新终端自动加载。
- [x] 确认 Maven 调用 SDKMAN 的 JDK。
- [x] 保持本地仓库为 `/Volumes/Work-Data/data/maven-repository`。
- [x] 完成 OryxOS 运行包构建。
- [x] 初始化工作区及数据库。
- [x] 定位到启动前缺少有效 Provider。
- [x] 准备本机配置与凭证文件，确认权限及 Git 忽略规则。
- [x] 填写并加载 DeepSeek API Key。
- [x] 成功启动服务，独立验证健康检查、端口与管理台。
- [x] 通过 DeepSeek 真实模型列表接口验证连通性和鉴权。
- [x] 填写飞书 Webhook 和签名密钥，注册 `homework-feishu` 渠道。
- [x] 创建并检查 `homework-hello`，仅启用 `notify` 工具。
- [x] 实际调用真实模型，成功执行一次 notify。
- [x] 核对执行记录、LLM 审计、工具审计及管理台执行历史。
- [x] 用户在飞书群确认消息送达。
- [x] 将实际结果整理回第二周第一题答案。

### 第一题完成情况

第一题要求的“克隆、运行、配置首个 Agent、执行并发出通知”均已完成。其他题目另行推进。

### 本次没有执行的事项

本次没有修改 OryxOS 业务源码，没有执行 Java 单元测试，没有提交、推送或部署。没有新增定时任务、额外 Skill、知识库或其他工具。服务继续在本机运行。

## 十四、复习时应该能讲清楚的问题

1. SDKMAN、JDK、Maven 分别负责什么？
2. Maven 软件目录和本地仓库为什么可以分开？
3. 为什么 `java -version` 正确，仍可能出现 Maven 使用另一份 JDK？
4. `.sdkmanrc`、`settings.xml`、`application.yml` 和 `.env` 各负责什么？
5. `source`、`export` 与 Java 进程继承环境变量是什么关系？
6. 为什么 `init` 成功、JAR 构建成功，都不能证明服务已经启动？
7. Provider 的连接配置与 Agent 的模型配置有什么区别？
8. 为什么通知渠道注册成功仍不能证明消息已送达？
9. 为什么修改 `.env` 后重启，不一定覆盖已经播种进数据库的 Provider？

复习目标：能够按“环境 → 构建 → 配置 → 启动 → Agent → 工具 → 实际送达”的顺序复述，并知道每一层用什么证据判断完成。

## 十五、依据与后续更新

本记录的已完成事实来自本机命令输出、文件校验、当前源码、管理台实际显示及用户的飞书收件确认。第一题已完成；以后重做或变更配置时，继续区分实际结果与操作示例。

主要参考：

- 本机项目 README（本机路径：`/Volumes/Work-Data/work/AI/projects/oryxos/README.md`）
- 本机项目 POM（本机路径：`/Volumes/Work-Data/work/AI/projects/oryxos/pom.xml`）
- SDKMAN 项目版本文件（本机路径：`/Volumes/Work-Data/work/AI/projects/oryxos/.sdkmanrc`）
- 启动脚本（本机路径：`/Volumes/Work-Data/work/AI/projects/oryxos/bin/start.sh`）
- Provider 启动校验（本机路径：`/Volumes/Work-Data/work/AI/projects/oryxos/oryxos-web/src/main/java/io/oryxos/web/security/ProviderStartupCheck.java`）
- Agent/Profile 本机说明（本机路径：`/Volumes/Work-Data/work/AI/projects/oryxos/website/zh/docs/profile.md`）
- 飞书通知适配器（本机路径：`/Volumes/Work-Data/work/AI/projects/oryxos/oryxos-tool/src/main/java/io/oryxos/tool/notify/FeishuNotifyAdapter.java`）
- 当前管理台源码（本机路径：`/Volumes/Work-Data/work/AI/projects/oryxos/oryxos-web/src/main/frontend/src/App.vue`）
- [SDKMAN 官方安装说明](https://sdkman.io/install/)
- [SDKMAN 官方使用说明](https://sdkman.io/usage/)
- [Maven 用户配置与本地仓库说明](https://maven.apache.org/settings.html)
- [DeepSeek 首次调用说明](https://api-docs.deepseek.com/zh-cn/)
- [飞书自定义机器人说明](https://open.feishu.cn/document/client-docs/bot-v3/add-custom-bot?lang=zh-CN)
