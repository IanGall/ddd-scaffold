#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/**
 * 对象转换定义。
 *
 * 1. 该包承接 Domain 模型与 API DTO 的映射定义。
 * 2. Domain 层不依赖 API，避免领域层反向依赖协议层。
 * 3. Trigger/RPC 层通过 Converter 执行转换。
 */
package ${package}.trigger.convertor;
