# ${uAppName} 网关

这是一个基于 Spring Boot、Dubbo（`dubbo` 协议）和 Nacos 的单模块网关骨架：认证转发与 RBAC 管理端代理都直接使用平台认证契约，
Gateway 自身不保存会话、不连接 Redis。

<h2>契约依赖</h2>

网关是「平台认证网关」，直接使用平台认证契约，因此 `pom.xml` 必须依赖认证契约制品：

- 契约制品：`ian-ddd-auth-api`（可用 `pom.xml` 的 `auth.contract.artifactId` 属性整体替换）
- 契约来源：共享契约仓 `ian-ddd-api/ian-ddd-api-internal/<service>-api`，版本由 `ddd-base-bom` 统一管理（本模板不写版本号）
- 用到的契约：`cn.iantech.api.*` 下的 `IAuthService` / `IRbacService` / `IPlatformAccountService` / `IChannelCredentialService`
  及 `model.{auth,customer,rbac,channel}.*`

网关**不定义也不自带** RPC 契约。新增或修改契约请改契约仓并同步 `ddd-base-bom`，不要在网关工程内新建 `api` 模块。

已代理的接口：

```text
/api/admin/auth/**                                # 登录、刷新、注销、会话
/api/admin/platform/accounts                      # 平台开户（X-Platform-Token）
/api/admin/platform/channel-credentials/**        # 渠道凭证与数据范围
/api/admin/rbac/users|roles|permissions/**        # 用户、角色、权限 CRUD
/api/admin/rbac/users/{id}/roles                  # 用户角色关系（GET/PUT）
/api/admin/rbac/roles/{id}/permissions            # 角色权限关系（GET/PUT）
/api/app/auth/**                                  # C 端注册、登录、刷新、注销、会话
/actuator/health                                  # 公开健康检查
```

<h2>启动</h2>

所有环境必须设置 `DUBBO_REGISTRY_ADDRESS`、`DUBBO_REGISTRY_USERNAME` 和 `DUBBO_REGISTRY_PASSWORD`。Access Token、 Refresh
Token 与 Session 均由 Auth 服务保存，RBAC 与 Customer 作为身份校验提供方，Gateway 不连接 Redis。

```bash
mvn spring-boot:run
```

`GET /actuator/health` 为公开健康检查。管理端登录使用 `POST /api/admin/auth/login`，C 端注册/登录使用
`POST /api/app/auth/register`、`POST /api/app/auth/login`，均返回 opaque Access Token 和 Refresh Token；业务请求只在
`Authorization: Bearer <accessToken>` 中携带 Access Token，Refresh Token 仅允许通过对应端的 `/api/*/auth/refresh` JSON
请求体使用。

```bash
LOGIN_RESPONSE=$(curl -s -X POST http://127.0.0.1:8092/api/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"loginName":"用户名@主账号ID.com","password":"账号密码"}')
ACCESS_TOKEN=$(printf '%s' "$LOGIN_RESPONSE" | jq -r '.data.accessToken')
REFRESH_TOKEN=$(printf '%s' "$LOGIN_RESPONSE" | jq -r '.data.refreshToken')
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://127.0.0.1:8092/api/admin/status

curl -s -X POST http://127.0.0.1:8092/api/admin/auth/refresh \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

每次刷新和业务请求都会调用 Auth 校验主账号、子账号状态和会话状态，并轮换 Refresh Token；旧 Refresh Token 重放会撤销
整个令牌族。管理端与 C 端的注销、注销全部和设备会话接口分别位于 `/api/admin/auth/**`、`/api/app/auth/**`，均要求有效 Access
Token。

生成网关只信任 Auth 返回的身份，并恢复主账号 ID、当前用户 ID 和本地用户名，不信任外部 `X-Account-Id`、`X-User-Id`。
`POST /api/admin/platform/accounts` 用于创建主账号。Gateway 只把 `X-Platform-Token` 和开户字段转发给独立的
`IPlatformAccountService`，平台凭据由 Provider 最终校验，Gateway 不保存或比较凭据。Dubbo 消费端使用明文 RPC， 注册中心通过
Nacos 用户名密码认证。

登录限流或临时锁定统一返回 `AUTH_RATE_LIMITED` 和 HTTP 429，不暴露具体触发条件。

`/api/external/**` 使用渠道 HMAC，不使用 Bearer Token。固定 scope 为 `external:access`。请求头固定为 `X-Channel-Code`、
`X-Channel-Secret-Version`、`X-Channel-Timestamp`、`X-Channel-Content-SHA256` 和 `X-Channel-Signature`。 Canonical Request
按 Method、Path、Query、Content-Type、渠道编码、密钥版本、时间戳和 Body SHA-256 八行组成， 签名算法固定为 HMAC-SHA256。请求体最大
1 MiB，时间窗为前后 300 秒；相同签名只能成功一次，重试必须更新时间戳并重新签名。

认证 RPC 的异常由 `GatewayAuthClient` 沿 cause 链保留 `AppException`，未声明的 RPC 失败统一转换为
`AUTH_UNAVAILABLE`。过滤器将异常委托给唯一的 `GatewayExceptionHandler`，由 `Constants.ResponseCode` 统一决定 HTTP 状态和
`data: null` 响应；不要在过滤器或控制器中手写 JSON、解析 Dubbo `GenericException` 或重复维护状态码映射。

<h2>Kubernetes 部署</h2>

生成工程自带一套原生 k8s 清单（不需要 Helm/Kustomize），位于 `dev-ops/k8s/`：

| 文件 | 内容 |
| --- | --- |
| `deployment.yaml` | 2 副本、HTTP 8092、`/actuator/health` 探针、emptyDir、优雅停机 |
| `service.yaml` | ClusterIP，`http 8092` |
| `configmap.yaml` | 非敏感配置（Nacos 地址与用户名） |
| `secret.yaml.example` | 敏感配置**键名样例**，不含真实值 |
| `hpa.yaml` | CPU 70%，2 → 6 |
| `pdb.yaml` | `minAvailable: 1` |
| `ingress.yaml` | 唯一对外入口，全量路径透传给网关 |

使用流程：

```bash
# 1. 构建镜像（在生成工程根目录执行；脚本自动识别本机架构，出 arm64 还是 amd64 无需手工指定）
bash build.sh                        # system/${rootArtifactId}:${version}

# 2. 创建凭证（真实值不入库）
kubectl create secret generic ${rootArtifactId}-secret -n <namespace> \
  --from-literal=DUBBO_REGISTRY_PASSWORD='...'

# 3. 按顺序部署
kubectl apply -n <namespace> -f dev-ops/k8s/configmap.yaml
kubectl apply -n <namespace> -f dev-ops/k8s/deployment.yaml
kubectl apply -n <namespace> -f dev-ops/k8s/service.yaml
kubectl apply -n <namespace> -f dev-ops/k8s/ingress.yaml
kubectl apply -n <namespace> -f dev-ops/k8s/hpa.yaml
kubectl apply -n <namespace> -f dev-ops/k8s/pdb.yaml
```

要点：

- `ingress.yaml` 的 `spec.ingressClassName` 与 `host` 都是占位，部署前按目标集群替换；Ingress 只做全量路径透传，
  路径白名单的唯一真相在应用侧，不要复制到 Ingress。
- 探针用 `GET /actuator/health`（actuator 只暴露 health）；网关不持有数据源，health 为 UP 即代表可服务。
- `secret.yaml.example` 的扩展名不是 `.yaml`，因此 `kubectl apply -f <目录>` 不会把它纳入；真实 Secret 用命令式创建。
- 修改 ConfigMap / Secret 后必须手动滚动重启：`kubectl rollout restart deployment/${rootArtifactId} -n <namespace>`。
- 客户端 IP：应用取的是连接地址（`getRemoteAddr()`），不采信客户端提交的 `X-Forwarded-For`。启用按 IP 的登录风控前，
  先在 Ingress Controller 层统一 strip 再重写该头，验证后再用部署层配置让应用信任代理（不要写进 `application.yml`）。
- 清单文件名固定为 `.yaml` / `.yaml.example`：新增文件类型时必须在骨架的 `archetype-metadata.xml` 里补对应的 `include`，
  否则文件不会进入生成工程（护栏 `scaffold-template-guard` 会拦截这种漏配）。
- 清单是 Velocity 模板（`filtered="true"`），模板变量会被替换成生成工程的实际值；**不要**在 YAML 正文里书写 shell 风格的
  变量占位符，环境变量一律通过 `envFrom` 注入。

<h2>分布式 E2E 覆盖率</h2>

本网关已内置接入分布式覆盖率所需的全部文件：

```text
dev-ops/start-with-coverage.sh                  # 以 jacocoagent tcpserver 模式启动，Agent 默认 6300
src/main/resources/application-coverage.yml    # 报告使用的 classes / source 目录
```

启动：

```bash
dev-ops/start-with-coverage.sh
```

E2E 测试写在网关侧（只调用网关 HTTP 接口），各服务 JVM 内的 Agent 自动记录自己被覆盖的情况。
完整接入步骤见 `ddd-base/ian-ddd-coverage/EXTENDING.md`，要点：

- 服务启动后，用注册接口把网关登记到控制器（无需改配置重启）：

  ```bash
  ddd-base/ian-ddd-coverage/coverage-e2e.sh register <服务名> 6300 \
    "$(pwd)/target/classes" "$(pwd)/src/main/java"
  ```
- Agent 名称默认取工程目录名，必须与注册时的服务名一致
- 端口约定：Gateway 6300、标准服务 6301、后续服务依次顺延
- 测试类加 `@CoversE2e` 注解，并用 `@EnabledIfEnvironmentVariable(named = "RUN_COVERAGE_E2E", matches = "true")` 控制开关
