#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.support.infra;

import ${package}.domain.support.model.event.BaseEvent;

/**
 * 事件消息接口
 */
public interface IEventProducer {
    <T> void asyncPub(String topic, BaseEvent<T> message);

    <T> void publish(String topic, BaseEvent<T> message);
}
