#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.domain.support.model.event;

import lombok.Data;

/**
 * 基础领域事件。
 */
@Data
public class BaseEvent<T> {

    private T data;
}
