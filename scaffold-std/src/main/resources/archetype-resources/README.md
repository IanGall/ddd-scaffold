# ian-frame-archetype - DDD 脚手架

<h2>分层边界</h2>

生成工程固定包含 `api`、`domain`、`infrastructure`、`trigger` 和 `boot` 五个业务模块，不再包含 Application 模块。

1. `domain.<业务域>` 保存聚合、实体、值对象、领域服务和 `infra` 基础设施能力契约，不依赖 API DTO、Context、RPC 或
   Infrastructure 实现。领域服务实现可使用 Spring `@Service` 参与组件扫描，但领域模型、值对象、聚合和 infra 接口不得依赖技术框架。
2. 与 `domain` 同级的 `cases.<业务域>.service` 负责事务、权限、审计和跨领域能力编排，可使用 Spring Service、Spring
   Transaction 与日志。
3. Cases 通过 `domain.<业务域>.infra` 中的契约访问基础设施，由 Infrastructure 提供实现；Domain 不再定义 `port`、
   `repository` 或 `adapter` 契约包。
4. Trigger 负责可信上下文解析和 API DTO 转换，只能调用 `domain.<业务域>.service` 或 `cases.<业务域>.service`。
5. `domain` 禁止依赖 `cases`；Cases 禁止依赖 API DTO、Context、RPC、Trigger 或 Infrastructure 实现。
6. `xxx` 是空业务域结构示例；复制并重命名后再实现业务，不保留无业务意义的重复占位域。

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

Starter 通过 Redis 租约自动分配并续租 Worker ID，不需要应用手工配置 Worker ID。由应用分配的数据库主键应显式写入 ID，不能同时
保留数据库自增。Session/Family 等非安全业务标识也通过领域端口适配该 Starter；Access Token、Refresh Token、渠道密钥和 AES IV
仍必须使用不可预测的安全随机值。`test` Profile 默认设置 `ddd.id-generator.enabled=false`，普通测试应提供确定性的 Fake ID
生成器。

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

<h2>分层自检</h2>

```bash
rg "import .*api\\.model|import .*context|import .*rpc|import .*trigger|import .*infrastructure" <your-project>-domain/src/main/java/cases
rg "import .*cases" <your-project>-domain/src/main/java/domain
rg "<artifactId><your-project>-api</artifactId>|<artifactId><your-project>-infrastructure</artifactId>" <your-project>-domain/pom.xml
rg "import .*application|<artifactId>.*-application</artifactId>" .
```

以上命令均应无输出。Trigger 的业务协作者应只来自 Domain Service 或 Cases Service。

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
