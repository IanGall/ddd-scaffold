# Infrastructure 模板模块协作说明

## 模块定位
- 本目录是生成项目中的基础设施层模板。
- 实现持久化、缓存、消息与远程调用等外部适配能力。

## 变更边界
- 允许修改：Repository 实现模板、Mapper 模板、客户端适配模板。
- 禁止修改：领域规则定义与 API 契约语义。

## 依赖约束
- 依赖 `domain` 与实际使用的技术框架，对上层通过接口暴露能力。
- Redis 访问统一依赖 `ddd-redis-starter` 提供的 `cn.iantech.redis.IRedisService`，不得在业务 Infrastructure 重复定义
  Redis 服务或直接依赖 `RedissonClient`。
- 避免技术细节向上层泄漏。
- 主账号 Repository 的每条 SQL 都必须显式接收 `Long accountId`，最终更新和删除语句也必须包含 `account_id` 条件。

## 提交前检查
- 评估调用次数与查询复杂度，避免明显性能回退。
- 删除废弃适配模板与无效配置项。
