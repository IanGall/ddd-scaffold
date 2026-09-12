# Trigger 模板模块协作说明

## 模块定位
- 本目录是生成项目中的入站触发层模板。
- 处理 HTTP/RPC/MQ 等入口请求，完成参数转换并转发到 Domain Service 或 Cases Service。

## 变更边界
- 允许修改：Controller 模板、消费者入口模板、请求响应转换模板。
- 禁止修改：核心业务编排、领域规则、基础设施实现。

## 依赖约束

- 依赖 `api` 与 `domain`，不依赖 `boot` 或 `infrastructure`。
- 仅做适配与转换，不承载复杂业务逻辑。
- 业务协作者只能是 `domain.<业务域>.service` 或 `cases.<业务域>.service` 下的服务。
- 不在 API DTO 增加通用 `accountId` 字段；Trigger 解析可信上下文并构造显式领域参数，授权由 Cases 负责。

## 提交前检查
- 参数校验完整，错误码语义一致。
- 清理重复转换逻辑与未使用入口模板。
