package com.example.demo.mq;

import com.example.demo.mq.event.TransferCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "${app.mq.topic}",
        selectorExpression = "completed",
        consumerGroup = "balance-transfer-consumer-completed")
public class TransferCompletedConsumer implements RocketMQListener<TransferCompletedEvent> {
    @Override
    public void onMessage(TransferCompletedEvent msg) {
        log.info("[MQ] transfer completed: id={}", msg.getTransferId());
    }
}
