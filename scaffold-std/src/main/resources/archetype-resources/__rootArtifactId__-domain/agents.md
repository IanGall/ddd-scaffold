# Domain 模板模块协作说明

## 模块定位
- 本目录是生成项目中的领域层模板。
- 承载聚合、实体、值对象、领域服务，以及与 `domain` 同级的 `cases` 用例编排。

## 变更边界

- 允许修改：领域模型模板、领域服务模板、领域事件模板和 Cases 用例服务。
- 禁止修改：接口协议、数据库映射与第三方客户端实现。

## 依赖约束

- Cases 可使用 Spring Service、事务和日志，负责权限校验、审计及多个 Domain infra 契约的编排。
- Cases 禁止依赖 API DTO、Context、RPC、Trigger 或 Infrastructure 实现；Domain 禁止反向依赖 Cases。
- `domain.<业务域>.infra` 统一定义由 Infrastructure 实现的技术无关契约，不保留 `port`、`repository` 或 `adapter` 契约包。
- 领域服务实现可使用 Spring `@Service` 自动注册为 Bean；核心领域模型、聚合、实体、值对象以及 infra 接口不依赖 Spring、API
  DTO、上下文模块、ThreadLocal、MyBatis、Redis、Dubbo、Servlet 或 Infrastructure 实现。
- 租户等业务隔离参数必须由 Trigger 解析为显式领域参数后传入 Cases。

## 提交前检查
- 关键规则覆盖正常与边界场景。
- 删除无效规则分支与冗余字段定义。
