# Scaffold 模块协作说明

## 模块定位
- 本模块用于脚手架模板编排与生成入口。
- 负责聚合各分层模板并输出标准项目骨架。

## 变更边界
- 允许修改：模板装配流程、脚手架参数映射、生成入口逻辑。
- 禁止修改：业务领域规则、具体基础设施实现细节。

## 依赖约束
- 仅依赖模板定义与必要的公共类型。
- 不直接承担业务编排职责，不沉淀领域决策。
- 与 Domain 同级的 Cases 包承担用例编排和事务边界，可依赖 Spring Context、Spring Transaction 与日志门面；领域服务实现可使用
  Spring `@Service` 自动注册。
- Domain 对基础设施的能力契约统一放入 `domain.<业务域>.infra`，不得生成 `port`、`repository` 或 `adapter` 契约包。
- Domain 核心模型、聚合、实体、值对象和 infra 接口不得依赖 Spring 或 Cases；Cases 不得依赖 API DTO、Context、RPC、Trigger 或
  Infrastructure 实现，只能通过 Domain infra 契约
  调用基础设施能力。
- Boot/Trigger 模板按边界引入 `ddd-context-dubbo`；Provider 使用明文 Triple，注册中心凭据不得写入模板。

## 提交前检查
- 确认生成流程可执行，输出目录结构完整。
- 确认生成工程不存在 Application 模块，Trigger 仅调用 Domain Service 或 Cases Service。
- 删除无用模板片段与废弃参数映射。
