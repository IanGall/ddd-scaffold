# ${rootArtifactId} - DDD 分层工程

<h2>分层边界</h2>

生成工程固定包含 `domain`、`infrastructure`、`trigger` 和 `boot` 四个模块，不再包含 Application 模块与本地 `api` 模块
（RPC 契约不在服务工程内定义，见下方「RPC 契约位置」）。

1. `domain.<业务域>` 保存聚合、实体、值对象、领域服务和 `infra` 基础设施能力契约，不依赖 API DTO、Context、RPC 或
   Infrastructure 实现。领域服务是**具体类**（用 Spring `@Service` 参与组件扫描），**不建 `I*` 接口**——接口只用于
   `infra` 端口/SPI（由 Infrastructure 或外部能力实现）。领域模型、值对象、聚合和 infra 接口不得依赖技术框架。
2. 与 `domain` 同级的 `cases.<业务域>.service` 负责事务、权限、审计和跨领域能力编排，可使用 Spring Service、Spring
   Transaction 与日志。
3. Cases 通过 `domain.<业务域>.infra` 中的契约访问基础设施，由 Infrastructure 提供实现；Domain 不再定义 `port`、
   `repository` 或 `adapter` 契约包。
4. Trigger 负责可信上下文解析和 API DTO 转换，只能调用 `domain.<业务域>.service` 或 `cases.<业务域>.service`。
5. `domain` 禁止依赖 `cases`；Cases 禁止依赖 API DTO、Context、RPC、Trigger 或 Infrastructure 实现。
6. 骨架只预置一个最小示例（`domain/user` 领域服务 + `cases/user` 用例编排）；其余业务域按同样结构新增，不生成无业务意义的空占位包。

<h2>骨架预置范围</h2>

为避免为不需要的能力背依赖，骨架**不预置**以下内容，需要时自行引入：

- 分库分表（ShardingSphere）：`spring.datasource` 为单库 MySQL
- 消息队列（Kafka）与任务调度（XXL-Job）
- 平台专属能力（如渠道密钥加密、防重放存储）

`domain.support.infra.IEventProducer` 与 `domain.support.model.event.BaseEvent` 保留为领域事件端口与模型；
具体 MQ 适配器由使用方在 `infrastructure` 侧实现。MyBatis 的 Mapper 放在
`<your-project>-infrastructure/src/main/resources/mybatis/mapper/`。

生成后的包结构：

```text
<your-project>-domain/src/main/java
├── domain/{业务域}/service          # 领域服务（内含 user 示例）
├── domain/support/infra             # 基础设施能力契约（端口），由 infrastructure 实现
├── domain/support/model/event       # 领域事件基类
└── cases/{业务域}/service           # 用例编排：事务、权限、审计、跨领域编排

<your-project>-infrastructure/src/main/java
└── （按需新增）持久化 / 外部系统 / MQ / 缓存等适配器实现

<your-project>-trigger/src/main/java
└── trigger/rpc                      # Dubbo Triple 服务提供者实现（@DubboService）

<your-project>-boot/src/main/java
├── config                           # 装配与启动期配置
└── <App>Application                 # 启动入口
```

<h2>RPC 契约位置</h2>

跨进程契约（Dubbo RPC 接口与其传输 DTO）**不在本工程内定义**，统一放在共享契约仓库，避免每个服务各持一份同名契约导致漂移：

```text
ian-ddd-api                                   # 契约聚合工程（独立于任何服务实现）
└── ian-ddd-api-internal                      # 内部：服务间 Dubbo RPC 契约
    └── <your-service>-api                    # 新增服务时在此登记一个契约子模块
└── ian-ddd-api-external                      # 外部：对外/第三方 HTTP 契约（@HttpExchange）
```

新增服务的做法：

1. 在 `ian-ddd-api/ian-ddd-api-internal` 下新增 `<your-service>-api` 子模块，存放 RPC 接口与 DTO。
2. 在 `ddd-base-bom` 中登记该契约模块，由 BOM 统一管理版本，消费方无需各自写版本号。
3. 服务工程与消费方（网关等）都只依赖契约制品，**不依赖对方的实现工程**。

判据：只有真正跨进程传输的类型才进契约仓；仅在本服务进程内使用的 DTO 或共享类型应放在 `domain`（或 `cases`）中，不要新建 `api` 模块。

<h2>全局唯一 ID</h2>

Infrastructure 已引入 `ddd-id-generator-starter`。在需要由应用生成业务 ID 的组件中注入公共接口：

```java
import cn.iantech.id.GlobalIdGenerator;

public class OrderRepository {

    private final GlobalIdGenerator globalIdGenerator;

    public OrderRepository(GlobalIdGenerator globalIdGenerator) {
        this.globalIdGenerator = globalIdGenerator;
    }

    public long nextOrderId() {
        return globalIdGenerator.nextId();
    }
}
```

Starter 通过 Redis 租约自动分配并续租 Worker ID，不需要应用手工配置 Worker ID。**不是所有表都需要它**：主键由应用生成时应显式写入
ID 且不要声明自增；内部字典表、从属数据、关联表可以继续用数据库自增——两种方式不要混在同一张表上。Session/Family 等非安全
业务标识也通过领域端口适配该 Starter；Access Token、Refresh Token 等安全令牌
仍必须使用不可预测的安全随机值。`test` Profile 默认设置 `ddd.id-generator.enabled=false`，普通测试应提供确定性的 Fake ID
生成器。需要让多个业务各自独占一段 Worker ID 区间时，声明 `ddd.id-generator.businesses` 并改为注入
`cn.iantech.id.GlobalIdGeneratorProvider`，用 `forBusiness(...)` 在构造期解析本业务的生成器。

<h2>Cases 示例</h2>

```java
package ${package}.cases.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCaseService {

    @Transactional(readOnly = true)
    public String queryUserInfo(String request) {
        return "查询用户信息";
    }
}
```

Trigger 通过构造器注入该服务，完成协议对象与领域参数转换后再调用。API DTO 与领域对象的 MapStruct Plus 映射应定义在
`trigger.convertor`，不得放入 Domain。

<h2>Domain Service 示例</h2>

```java
@Service
public class UserDomainService {

    public String queryUserInfo(String request) {
        return "查询用户信息：" + request;
    }
}
```

Domain Service 只承载领域规则；事务、权限、审计和跨领域编排仍由同级 `cases` 服务负责。

Domain Service 与 Cases 都直接依赖**具体类**，不为其建接口：同模块单实现接口只是双份签名，不带来解耦。
只有当实现需要跨模块（`infra` 端口由 Infrastructure 实现）或存在多种实现时，才定义接口——例如
`domain.support.infra.IEventProducer`。

<h2>分层自检</h2>

```bash
rg "import .*api\\.model|import .*context|import .*rpc|import .*trigger|import .*infrastructure" <your-project>-domain/src/main/java/cases
rg "import .*cases" <your-project>-domain/src/main/java/domain
rg "public interface I" <your-project>-domain/src/main/java/domain -g '**/service/**'
rg "<artifactId><your-project>-infrastructure</artifactId>" <your-project>-domain/pom.xml
rg "import .*application|<artifactId>.*-application</artifactId>" .
```

以上命令均应无输出。Trigger 的业务协作者应只来自 Domain Service 或 Cases Service；领域服务（`service` 包）不得出现接口。

<h2>分布式 E2E 覆盖率</h2>

本工程已内置接入分布式覆盖率所需的全部文件，无需额外改动：

```text
docs/dev-ops/start-with-coverage.sh                     # 以 jacocoagent tcpserver 模式启动
<your-project>-boot/src/main/resources/
  └── application-coverage.yml                          # 报告使用的 classes / source 目录
```

启动（Agent 默认监听 127.0.0.1:6301）：

```bash
docs/dev-ops/start-with-coverage.sh
```

覆盖率由 `ddd-base/ian-ddd-coverage` 的控制器统一采集，完整接入说明见该模块的 `EXTENDING.md`。要点：

- 服务启动后，用注册接口把本服务登记到控制器（无需改配置重启）：

  ```bash
  ddd-base/ian-ddd-coverage/coverage-e2e.sh register <服务名> 6301 \
    "$(pwd)/<rootArtifactId>-trigger/target/classes,$(pwd)/<rootArtifactId>-domain/target/classes" \
    "$(pwd)/<rootArtifactId>-trigger/src/main/java,$(pwd)/<rootArtifactId>-domain/src/main/java"
  ```
- Agent 名称默认取工程目录名，必须与注册时的服务名一致
- 端口约定：Gateway 6300、标准服务 6301、后续服务从 6302 起顺延
- 拆分出新模块时，记得把它补进注册的目录列表（以及 `application-coverage.yml`）
- 改完代码必须 clean 重建再重启服务，否则 classId 不一致会让覆盖率静默变成 0%

<h2>上下文与租户隔离</h2>

- Trigger 和 Provider 按边界引入 Web/Dubbo 上下文适配器，并将可信上下文转换为显式领域参数。
- 主账号身份必须来自受校验的认证上下文或 Claim，禁止信任外部 `X-Account-Id`、`X-User-Id` 请求头。
- 生产 Provider 使用明文 Triple RPC；Nacos 注册中心凭据仅通过 Secret 或环境变量注入。
