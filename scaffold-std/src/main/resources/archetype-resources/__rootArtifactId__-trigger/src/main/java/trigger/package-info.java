#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * Trigger 层：入站适配与入口编排。
 *
 * - rpc：Dubbo Triple 服务提供者实现（{@code @DubboService}），消费方为网关等上游
 *
 * 需要 HTTP 入口、任务调度或 MQ 监听时，在本层按需新增对应包（如 web / job / listener）。
 * 骨架不预置具体中间件实现，避免为不需要的能力背依赖。
 */
package ${package}.trigger;
