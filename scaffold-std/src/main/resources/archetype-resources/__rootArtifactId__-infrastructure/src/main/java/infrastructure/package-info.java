#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * Infrastructure 层：出站适配器实现（Domain 声明端口，本层提供实现）。
 *
 * 常见放置位置：
 *
 * - persistent：MyBatis DAO / PO / Repository 实现；Mapper XML 放
 *   {@code src/main/resources/mybatis/mapper/}
 * - gateway：调用外部 HTTP / RPC 系统的客户端与 DTO
 * - event：MQ 事件生产适配器（实现 {@code domain.support.infra.IEventProducer}）
 * - 其他能力（缓存、对象存储等）按能力新建子包
 *
 * 本层只实现 domain 侧声明的端口，不反向定义业务规则；骨架不预置具体中间件实现。
 */
package ${package}.infrastructure;
