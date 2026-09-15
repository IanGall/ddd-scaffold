# ddd-scaffold

DDD 工程骨架（Maven Archetype）聚合工程。

骨架负责生成新的标准服务与新网关，[`ddd`](https://github.com/IanGall/ddd) 仓库提供基座制品与参考实现。两个仓库必须同级检出：
本仓库 `pom.xml` 的 parent 经 `relativePath` 指向 `../ddd/ddd-base/pom.xml`，生成的工程继承 `ddd-base` 并导入 `ddd-base-bom`。

## 骨架制品

| 骨架              | 坐标                                      | 生成的工程                                       |
|-------------------|-------------------------------------------|--------------------------------------------------|
| `scaffold-std`    | `cn.iantech:scaffold-std:1.0-SNAPSHOT`    | 标准服务五模块：api / domain / infrastructure / trigger / boot |
| `scaffold-gateway`| `cn.iantech:scaffold-gateway:1.0-SNAPSHOT`| 单模块网关：Web 接入、Auth RPC 认证、统一异常等   |

参考实现对应关系：`ian-ddd-auth` ↔ `scaffold-std`，`ian-ddd-gateway` ↔ `scaffold-gateway`。
骨架只保留通用能力与少量示例业务域，完整业务（Auth、RBAC 等）在参考实现中，不复制进骨架。

## 仓库结构

```text
ddd-scaffold/
├── pom.xml            # 聚合 POM（artifactId ddd-scaffold-parent，parent 指向 ../ddd/ddd-base）
├── scaffold-std/      # 标准服务骨架（Maven Archetype）
└── scaffold-gateway/  # 网关骨架（Maven Archetype）
```

## 环境要求

- JDK 21、Maven 3.9.x。
- `ddd` 仓库同级检出，且基座制品已安装到本地 Maven 仓库。

## 构建与验证

先在 `ddd` 仓库根目录安装基座制品：

```bash
mvn -B install -DskipTests
```

再在 `ddd-scaffold` 仓库根目录构建并验证两个骨架：

```bash
mvn -B verify
```

也可以只验证单个骨架：

```bash
mvn -f scaffold-std/pom.xml clean verify
mvn -f scaffold-gateway/pom.xml clean verify
```

`verify` 会按 `src/test/resources/projects` 的配置生成临时工程并编译：

- `scaffold-std` 的 Smoke 工程验证 api / domain / infrastructure / trigger / boot 五个模块可协作构建，并断言不存在多余的 Application 模块；
- `scaffold-gateway` 的测试网关执行上下文测试、HTTP 安全链路测试与 RPC 异常映射测试。

此外 `scaffold-template-guard` 模块对两个骨架做模板静态护栏：未开启 Velocity 过滤的 fileSet 不得含转义占位符，
且 `archetype-resources` 下每个文件（模块 pom 除外）都必须被某个 fileSet 命中——历史上出现过 include 漏配导致文件从不生成的事故。

## 使用骨架生成工程

以标准服务为例（网关参数见 [scaffold-gateway/README.md](scaffold-gateway/README.md)）：

```bash
mvn -f scaffold-std/pom.xml clean install

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

cd demo && mvn clean package
```

生成的标准服务默认不包含 HTTP 触发能力，需要时在工程根目录构建启用 `-Phttp`。
完整参数、生成工程的分层约束与默认能力说明见 [scaffold-std/README.md](scaffold-std/README.md)。

## 发布到 RDC 制品仓库

先配置 `~/.m2/settings.xml` 的 RDC 认证与 `rdc` Profile，再执行：

```bash
mvn -f scaffold-std/pom.xml clean deploy -Prdc
mvn -f scaffold-gateway/pom.xml clean deploy -Prdc
```

仓库地址、账号配置与常见发布错误见 [scaffold-std/README.md](scaffold-std/README.md) 第 4 ～ 6 节。

## CI

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) 在 push 任意分支与 PR 时触发：以子目录布局同时检出
`ddd-scaffold` 与 `IanGall/ddd`，先安装 ddd 制品，再执行 `mvn -B -f ddd-scaffold/pom.xml verify`。

## 文档

- [scaffold-std/README.md](scaffold-std/README.md) — 标准服务骨架：构建、生成、RDC 发布与默认能力
- [scaffold-gateway/README.md](scaffold-gateway/README.md) — 网关骨架：构建、生成与认证契约
- [`ddd` 仓库](https://github.com/IanGall/ddd) — 基座组件、标准服务参考实现、网关参考应用与覆盖率体系
