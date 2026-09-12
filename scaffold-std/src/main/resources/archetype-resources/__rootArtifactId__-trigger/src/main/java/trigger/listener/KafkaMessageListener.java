#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\\' )
package ${package}.trigger.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka 消息监听器。
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KafkaMessageListener {

    @KafkaListener(topics = "${symbol_dollar}{kafka.topic:ian-mq}",
            groupId = "${symbol_dollar}{spring.kafka.consumer.group-id:ian-group}")
    public void onMessage(String message) {
        log.info("接收到 Kafka 消息 {}", message);
    }
}
