#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.infrastructure.event;

import ${package}.domain.support.infra.IEventProducer;
import ${package}.domain.support.model.event.BaseEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import jakarta.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Kafka 事件消息生产者。
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EventProducer implements IEventProducer {

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 异步消息
     *
     * @param topic   主题
     * @param message 消息
     */
    @Override
    @SneakyThrows
    public <T> void asyncPub(String topic, BaseEvent<T> message) {
        String mqMessage = objectMapper.writeValueAsString(message);
        log.info("发送 Kafka 异步消息 topic:{} message:{}", topic, mqMessage);
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, mqMessage);
        future.whenComplete((sendResult, exception) -> {
            if (exception != null) {
                log.error("发送 Kafka 异步消息失败 topic:{}", topic, exception);
                return;
            }
            log.info("发送 Kafka 异步消息成功 topic:{} partition:{} offset:{}", topic,
                    sendResult.getRecordMetadata().partition(), sendResult.getRecordMetadata().offset());
        });
    }

    /**
     * 普通消息
     *
     * @param topic   主题
     * @param message 消息
     */
    @Override
    public <T> void publish(String topic, BaseEvent<T> message) {
        try {
            String mqMessage = objectMapper.writeValueAsString(message);
            log.info("发送 Kafka 消息 topic:{} message:{}", topic, mqMessage);
            SendResult<String, String> sendResult = kafkaTemplate.send(topic, mqMessage).get();
            log.info("发送 Kafka 消息成功 topic:{} partition:{} offset:{}", topic,
                    sendResult.getRecordMetadata().partition(), sendResult.getRecordMetadata().offset());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待 Kafka 消息发送结果时线程被中断", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Kafka 消息发送失败，topic: " + topic, exception.getCause());
        } catch (Exception exception) {
            throw new IllegalStateException("Kafka 消息序列化失败，topic: " + topic, exception);
        }
    }
}
