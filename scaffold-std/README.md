# scaffold-std DDD 脚手架

本模块用于构建和发布 Maven Archetype。当前制品坐标如下：

```text
cn.iantech:scaffold-std:1.0-SNAPSHOT
```

## 1. 环境要求

- JDK 21
- Maven 3.9.x
- 本地 Maven 仓库能够解析骨架依赖
- 推送远程制品前，`~/.m2/settings.xml` 已配置 RDC 认证和 `rdc` Profile

以下命令默认在 `ddd-scaffold` 仓库根目录执行（`ddd` 仓库需同级检出，以便解析 `ddd-base` 父 POM）。

## 2. 构建骨架

### 2.1 打包并执行骨架集成测试

```bash
mvn -f scaffold-std/pom.xml clean verify
```

该命令会：

1. 构建 `scaffold-std-1.0-SNAPSHOT.jar`；
2. 根据 `src/test/resources/projects` 中的配置生成测试工程；
3. 编译生成的 Smoke 工程，验证 `api`、`domain`、`infrastructure`、`trigger` 和 `boot` 五个模块可以正常协作，并断言不存在
   Application 模块。

构建产物位于：

```text
scaffold-std/target/scaffold-std-1.0-SNAPSHOT.jar
```

### 2.2 安装到本地 Maven 仓库

```bash
mvn -f scaffold-std/pom.xml clean install
```

安装后的本地坐标为：

```text
cn.iantech:scaffold-std:1.0-SNAPSHOT
```

## 3. 使用骨架

### 3.1 使用本地安装的骨架

先在脚手架项目根目录执行本地安装：

```bash
mvn -f scaffold-std/pom.xml clean install
```

然后在准备生成项目的目录执行：

```bash
mvn archetype:generate \
  -DarchetypeCatalog=local \
  -DarchetypeGroupId=cn.iantech \
  -DarchetypeArtifactId=scaffold-std \
  -DarchetypeVersion=1.0-SNAPSHOT \
  -DgroupId=cn.example \
  -DartifactId=demo \
  -DrootArtifactId=demo \
  -DuAppName=Demo \
  -Dversion=1.0.0-SNAPSHOT \
  -Dpackage=cn.example.demo \
  -DinteractiveMode=false
```

命令执行后会在当前目录生成 `demo` 工程。进入生成工程后构建：

```bash
cd demo
mvn clean package
```

### 3.2 使用 RDC 远程 Snapshot 骨架

先完成 RDC 认证和 Profile 配置，然后在 `rdc` Profile 中增加远程仓库声明，供 Maven 下载 Snapshot：

```xml
<repositories>
    <repository>
        <id>2214-snapshot-OJo8ED</id>
        <url>https://ian1engineering-cn-shanghai.devops.aliyuncs.com/packages/api/protocol/maven/2214-snapshot-ojo8ed</url>
        <releases>
            <enabled>false</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

使用远程骨架：

```bash
mvn -Prdc archetype:generate \
  -DarchetypeCatalog=remote \
  -DarchetypeGroupId=cn.iantech \
  -DarchetypeArtifactId=scaffold-std \
  -DarchetypeVersion=1.0-SNAPSHOT \
  -DgroupId=cn.example \
  -DartifactId=demo \
  -DrootArtifactId=demo \
  -DuAppName=Demo \
  -Dversion=1.0.0-SNAPSHOT \
  -Dpackage=cn.example.demo \
  -DinteractiveMode=false
```

如果本地同时安装过同坐标的骨架，远程使用时保留 `-DarchetypeCatalog=remote`，避免误选本地版本。

生成的模块结构为：

```text
demo/
├── demo-api
├── demo-boot
├── demo-domain
├── demo-infrastructure
└── demo-trigger
```

其中：

- `boot`：Spring Boot 启动、配置和组件装配；
- `domain`：领域模型、领域服务和 `domain.<业务域>.infra` 基础设施能力契约；同一 Maven 模块内与 `domain` 同级的 `cases`
  包负责用例编排和事务边界；
- `infrastructure`：实现 Domain 定义的 infra 契约；Redis 基础能力由公共 `ddd-redis-starter` 自动装配，不再内置 Redis 接口和
  Redisson 实现；全局唯一 ID 由 `ddd-id-generator-starter` 提供，业务通过注入
  `cn.iantech.id.GlobalIdGenerator` 后调用 `nextId()` 获取；
- `trigger`：定时任务、消息监听、RPC 等外部触发入口，只调用 Domain Service 或 Cases Service。

ID Starter 依赖 Redis 租约自动分配 Worker ID，不要求应用手工指定 Worker ID。**不是所有表都需要该 Starter**：主键由应用生成时
必须显式写入 ID 且不要声明自增；内部字典表、从属数据、关联表可以继续用数据库自增，两者不要混在同一张表上（判据见
`ddd-base/README.md` 的「是否需要全局 ID」）。Session/Family 等非安全业务标识通过领域端口适配该 Starter。Access/Refresh Token、
渠道密钥及 AES IV 继续使用安全随机值。生成工程的
`test` Profile 默认禁用真实租约，普通测试应提供确定性的 Fake ID 生成器。需要按业务隔离 Worker ID 时，可声明
`ddd.id-generator.businesses` 并改注入 `cn.iantech.id.GlobalIdGeneratorProvider`，用法见 `ddd-base/README.md`。

## 4. 配置 RDC 制品仓库

在 `~/.m2/settings.xml` 中配置认证。`server.id` 必须与部署仓库 ID 完全一致：

```xml
<servers>
    <server>
        <id>2214-snapshot-OJo8ED</id>
        <username>你的仓库用户名</username>
        <password>你的仓库密码或访问令牌</password>
    </server>
</servers>
```

在同一个 `settings.xml` 的 `<profiles>` 中配置 Snapshot 发布地址：

```xml
<profiles>
    <profile>
        <id>rdc</id>
        <properties>
            <altSnapshotDeploymentRepository>
                2214-snapshot-OJo8ED::https://ian1engineering-cn-shanghai.devops.aliyuncs.com/packages/api/protocol/maven/2214-snapshot-ojo8ed
            </altSnapshotDeploymentRepository>
        </properties>
    </profile>
</profiles>
```

注意事项：

- Maven Deploy Plugin 3.x 使用 `id::url` 格式，不再使用旧版 `id::default::url`。
- Snapshot 仓库只能发布以 `-SNAPSHOT` 结尾的版本。
- 账号、密码或访问令牌只允许保存在本机 `settings.xml`，禁止写入项目 POM 或提交到 Git。
- `settings.xml` 中的 `<repositories>` 和 `<pluginRepositories>` 必须位于某个 `<profile>` 内，不能直接放在 `<settings>` 根节点下。

## 5. 推送到 RDC 制品仓库

执行：

```bash
mvn -f scaffold-std/pom.xml clean deploy -Prdc
```

在 IntelliJ IDEA 的 Maven Run Configuration 中，对应命令为：

```text
clean deploy -Prdc
```

发布成功时，日志应包含类似内容：

```text
Using alternate deployment repository 2214-snapshot-OJo8ED::https://...
Uploaded to 2214-snapshot-OJo8ED: .../cn/iantech/scaffold-std/1.0-SNAPSHOT/...
BUILD SUCCESS
```

远程 Snapshot 会保存为带时间戳的物理文件，例如：

```text
scaffold-std-1.0-20260811.071751-1.jar
```

使用方仍然引用逻辑版本：

```text
1.0-SNAPSHOT
```

## 6. 常见发布错误

### 6.1 未指定部署仓库

```text
repository element was not specified
```

确认命令包含 `-Prdc`，并确认 `rdc` Profile 中配置了 `altSnapshotDeploymentRepository`。

### 6.2 仓库返回 HTTP 400

优先检查版本与仓库类型是否匹配：

- `1.0-SNAPSHOT` 发布到 Snapshot 仓库；
- `1.0` 发布到 Release 仓库；
- 不要将正式版本发布到 Snapshot 仓库。

### 6.3 settings.xml 出现 Unrecognised tag

检查 `<repositories>` 或 `<pluginRepositories>` 是否被错误放到了 `<settings>` 根节点。它们只能配置在 `<profile>` 内。

## 7. 构建生成后的工程

进入脚手架生成的项目根目录后，执行完整构建：

```bash
mvn clean package
```

如果生成工程需要 HTTP 接口能力，启用 `http` Profile：

```bash
mvn clean package -Phttp
```

只需要快速验证编译、不执行测试时使用：

```bash
mvn clean package -DskipTests
```

## 8. 默认能力说明

- 标准脚手架生成的工程默认不包含 HTTP 触发能力，不内置 Spring MVC 依赖与示例 Controller。
- 如需 HTTP 接口能力，请在生成工程根目录构建时启用 `-Phttp`，为 `*-trigger` 模块引入 `spring-boot-starter-web`。
- 启用 `-Phttp` 后，再在 `*-trigger` 模块补充控制器实现。
- 生成的工程自带 `docs/dev-ops/k8s/` 部署清单（Deployment / Service / ConfigMap / Secret 示例 / HPA / PDB）。探针与 Service
  端口都指向 Dubbo 20880——默认能力下没有 servlet 容器，`server.port` 不会被监听；这些模板开启了 Velocity 过滤，改动时不要
  在 YAML 正文书写 shell 风格的变量占位符。
