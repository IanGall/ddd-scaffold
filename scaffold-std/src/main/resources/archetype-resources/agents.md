# 标准 DDD 工程协作说明

## 模块定位
- 本工程由 `scaffold-std` 脚手架生成，按 `domain/infrastructure/trigger/boot` 分层组织。
- 公共基础能力统一复用 `ddd-common`，禁止在模块内重复实现已有能力。

## 变更边界
- 允许修改：本工程的业务实现、模块构建配置与分层结构内的实现细节。
- 禁止修改：与当前需求无关的其它模块代码。

## 协作约束

- 分层职责清晰：`domain/infrastructure/trigger/boot` 各司其职，公共基础能力统一复用 `ddd-common`。
- RBAC 数据按主账号 `account_id` 隔离；Cases 完成主账号/子账号权限授权，Infrastructure 的每条 SQL 都必须带账号条件。
- 旧租户表和管理员专用认证不保留兼容层，必须直接采用主账号/子账号模型。
- 删除无用配置与废弃文件，避免冗余。

## 提交前检查
- 验证模块结构完整，关键模板目录可用。
- 确认变更不引入循环依赖或明显构建风险。
- 检查 Domain 未引入 Cases、上下文、Servlet、Dubbo 或基础设施实现；Spring `@Service` 仅允许用于领域服务实现和 `cases` 服务。
- Domain 对基础设施的能力契约统一放在 `domain.<业务域>.infra`，不得保留 `port`、`repository` 或 `adapter` 契约包。
- 改动 `docs/dev-ops/k8s/**` 后跑一次 `kubectl apply --dry-run=server -n <ns> -f docs/dev-ops/k8s/`；新增 `docs/dev-ops/` 子目录必须同步补 `.gitignore` 的 `!` 白名单，否则文件会被静默忽略。
- 禁止把真实凭证写入仓库：Secret 只保留 `secret.yaml.example`，真实值用 `kubectl create secret` 命令式创建。
- k8s 清单的探针与端口映射指向 Dubbo 20880；未启用 `-Phttp` 时工程没有 servlet 容器，按 `server.port` 配探针会让 Pod 进入 CrashLoopBackOff。
