package com.example.demo.mq;

import com.example.demo.mq.event.TransferCanceledEvent;
import com.example.demo.mq.event.TransferCompletedEvent;
import com.example.demo.mq.event.TransferCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferEventPublisher {

    private final RocketMQTemplate rocketMQTemplate;

    @Value("${app.mq.topic}")
    private  String transferTopic;

    public void publishCreated(TransferCreatedEvent event) {
        try {
            rocketMQTemplate.send(transferTopic + ":created", MessageBuilder.withPayload(event).build());
            log.info("[MQ]send transfer created: {} -> {} amount={} id={}", event.getFromUserId(), event.getToUserId(), event.getAmount(), event.getTransferId());
        } catch (Exception e) {
            log.error("failed to publish transfer created event: {}", event, e);
            throw e;
        }
    }

    public void publishCompleted(TransferCompletedEvent event) {
        try {
            rocketMQTemplate.send(transferTopic + ":completed", MessageBuilder.withPayload(event).build());
            log.info("[MQ]send transfer completed: {} -> {} amount={} id={}", event.getFromUserId(), event.getToUserId(), event.getAmount(), event.getTransferId());
        } catch (Exception e) {
            log.error("failed to publish transfer completed event: {}", event, e);
            throw e;
        }
    }

    public void publishCanceled(TransferCanceledEvent event) {
        try {
            rocketMQTemplate.send(transferTopic + ":canceled", MessageBuilder.withPayload(event).build());
            log.info("[MQ]send transfer canceled: {} -> {} amount={} id={}", event.getFromUserId(), event.getToUserId(), event.getAmount(), event.getTransferId());
        } catch (Exception e) {
            log.error("failed to publish transfer canceled event: {}", event, e);
            throw e;
        }
    }
}
